#include "nebula/jvm/jvm_manager.hpp"
#include "nebula/core/logger.hpp"
#include <algorithm>

namespace nebula {
namespace jvm {

static JavaVM*    g_vm             = nullptr;
static jvmtiEnv*   g_jvmti          = nullptr;
static jobject     g_gameLoader     = nullptr;
static jclass      g_hookManagerCls = nullptr;
static jmethodID   g_dispatchMid    = nullptr;

static CRITICAL_SECTION      g_hookLock;
static std::set<std::string> g_hookTargets;
static thread_local bool     t_inHook = false;

ScopedJniEnv::ScopedJniEnv() {
    if (!g_vm) return;
    jint res = g_vm->GetEnv(reinterpret_cast<void**>(&m_env), JNI_VERSION_1_8);
    if (res == JNI_EDETACHED) {
        JavaVMAttachArgs args;
        args.version = JNI_VERSION_1_8;
        args.name = const_cast<char*>("Nebula-Client-NativeThread");
        args.group = nullptr;
        if (g_vm->AttachCurrentThread(reinterpret_cast<void**>(&m_env), &args) == JNI_OK) {
            m_attached = true;
        }
    } else if (res != JNI_OK) {
        m_env = nullptr;
    }
}

ScopedJniEnv::~ScopedJniEnv() {
    if (m_attached && g_vm) {
        g_vm->DetachCurrentThread();
    }
}

static void JNICALL onClassFileLoad(jvmtiEnv* jvmti, JNIEnv* env,
                                    jclass redefined, jobject loader, const char* name,
                                    jobject domain, jint len, const unsigned char* data,
                                    jint* newLen, unsigned char** newData) {
    if (!name || !g_hookManagerCls || !g_dispatchMid || t_inHook) return;

    bool wanted = false;
    {
        ScopedLock lock(&g_hookLock);
        wanted = (g_hookTargets.find(name) != g_hookTargets.end());
    }
    if (!wanted) return;

    t_inHook = true;

    jstring jname = env->NewStringUTF(name);
    jbyteArray inputBytes = env->NewByteArray(len);
    env->SetByteArrayRegion(inputBytes, 0, len, reinterpret_cast<const jbyte*>(data));

    jbyteArray transformed = reinterpret_cast<jbyteArray>(
        env->CallStaticObjectMethod(g_hookManagerCls, g_dispatchMid,
                                    loader, jname, redefined, domain, inputBytes)
    );

    if (!clearException(env, "HookManager.dispatch") && transformed) {
        jint outLen = env->GetArrayLength(transformed);
        unsigned char* outBuf = nullptr;
        if (jvmti->Allocate(outLen, &outBuf) == JVMTI_ERROR_NONE) {
            env->GetByteArrayRegion(transformed, 0, outLen, reinterpret_cast<jbyte*>(outBuf));
            *newData = outBuf;
            *newLen = outLen;
            logger::info("Hook aplicado: %s (%d bytes)", name, outLen);
        }
        env->DeleteLocalRef(transformed);
    }

    env->DeleteLocalRef(inputBytes);
    env->DeleteLocalRef(jname);

    t_inHook = false;
}

bool clearException(JNIEnv* env, const char* context) {
    if (!env || !env->ExceptionCheck()) return false;
    jthrowable ex = env->ExceptionOccurred();
    env->ExceptionClear();
    if (ex) {
        jclass cls = env->GetObjectClass(ex);
        jmethodID toStringMid = env->GetMethodID(cls, "toString", "()Ljava/lang/String;");
        jstring str = toStringMid ? reinterpret_cast<jstring>(env->CallObjectMethod(ex, toStringMid)) : nullptr;
        env->ExceptionClear();
        if (str) {
            const char* c = env->GetStringUTFChars(str, nullptr);
            logger::error("[JNI %s] Excecao: %s", context, c ? c : "null");
            env->ReleaseStringUTFChars(str, c);
            env->DeleteLocalRef(str);
        }
        env->DeleteLocalRef(cls);
        env->DeleteLocalRef(ex);
    }
    return true;
}

bool isExceptionOfType(JNIEnv* env, const char* className) {
    if (!env || !env->ExceptionCheck()) return false;
    jthrowable ex = env->ExceptionOccurred();
    env->ExceptionClear();
    jclass type = env->FindClass(className);
    bool result = type && env->IsInstanceOf(ex, type);
    if (type) env->DeleteLocalRef(type);
    env->DeleteLocalRef(ex);
    return result;
}

static bool setupJvmti() {
    if (g_vm->GetEnv(reinterpret_cast<void**>(&g_jvmti), JVMTI_VERSION_1_2) != JNI_OK || !g_jvmti) {
        logger::error("GetEnv(JVMTI) falhou");
        return false;
    }

    jvmtiCapabilities caps;
    memset(&caps, 0, sizeof(caps));
    caps.can_retransform_classes = 1;
    caps.can_redefine_classes = 1;
    caps.can_generate_all_class_hook_events = 1;
    jvmtiError err = g_jvmti->AddCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        logger::error("AddCapabilities JVMTI falhou: %d", static_cast<int>(err));
        return false;
    }

    jvmtiEventCallbacks cb;
    memset(&cb, 0, sizeof(cb));
    cb.ClassFileLoadHook = &onClassFileLoad;
    g_jvmti->SetEventCallbacks(&cb, sizeof(cb));

    err = g_jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, nullptr);
    if (err != JVMTI_ERROR_NONE) {
        logger::error("SetEventNotificationMode falhou: %d", static_cast<int>(err));
        return false;
    }

    logger::info("JVMTI configurado com sucesso.");
    return true;
}

static jobject findGameLoader(JNIEnv* env) {
    jint threadCount = 0;
    jthread* threads = nullptr;
    if (g_jvmti->GetAllThreads(&threadCount, &threads) != JVMTI_ERROR_NONE) return nullptr;

    jclass threadCls  = env->FindClass("java/lang/Thread");
    jclass loaderCls  = env->FindClass("java/lang/ClassLoader");
    jclass classCls   = env->FindClass("java/lang/Class");
    jmethodID getCtx  = env->GetMethodID(threadCls, "getContextClassLoader", "()Ljava/lang/ClassLoader;");
    jmethodID load    = env->GetMethodID(loaderCls, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
    jmethodID getLdr  = env->GetMethodID(classCls, "getClassLoader", "()Ljava/lang/ClassLoader;");
    jstring mcName    = env->NewStringUTF("net.minecraft.client.Minecraft");

    jobject result = nullptr;
    for (jint i = 0; i < threadCount && !result; i++) {
        jobject ctx = env->CallObjectMethod(threads[i], getCtx);
        if (clearException(env, "getContextClassLoader") || !ctx) continue;
        jobject mc = env->CallObjectMethod(ctx, load, mcName);
        if (!clearException(env, "loadClass(Minecraft)") && mc) {
            jobject defining = env->CallObjectMethod(mc, getLdr);
            if (defining) result = env->NewGlobalRef(defining);
            env->DeleteLocalRef(defining);
        }
        env->DeleteLocalRef(mc);
        env->DeleteLocalRef(ctx);
    }

    g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(threads));
    env->DeleteLocalRef(mcName);
    env->DeleteLocalRef(classCls);
    env->DeleteLocalRef(loaderCls);
    env->DeleteLocalRef(threadCls);

    return result;
}

bool init() {
    InitializeCriticalSection(&g_hookLock);

    HMODULE jvmModule = GetModuleHandleA("jvm.dll");
    typedef jint(JNICALL* GetVMsFn)(JavaVM**, jsize, jsize*);
    GetVMsFn getVMs = jvmModule ? reinterpret_cast<GetVMsFn>(GetProcAddress(jvmModule, "JNI_GetCreatedJavaVMs")) : nullptr;

    jsize count = 0;
    if (!getVMs || getVMs(&g_vm, 1, &count) != JNI_OK || count < 1) {
        logger::error("Nenhuma JVM encontrada no processo");
        return false;
    }

    ScopedJniEnv env;
    if (!env.isValid()) {
        logger::error("Falha ao obter JNIEnv");
        return false;
    }

    if (!setupJvmti()) {
        return false;
    }

    g_gameLoader = findGameLoader(env.get());
    if (!g_gameLoader) {
        logger::error("ClassLoader do Minecraft nao encontrado");
        return false;
    }

    logger::info("JVM e ClassLoader do jogo inicializados com sucesso.");
    return true;
}

void shutdown() {
    ScopedJniEnv env;
    if (env.isValid()) {
        if (g_gameLoader) {
            env->DeleteGlobalRef(g_gameLoader);
            g_gameLoader = nullptr;
        }
        if (g_hookManagerCls) {
            env->DeleteGlobalRef(g_hookManagerCls);
            g_hookManagerCls = nullptr;
        }
    }
    DeleteCriticalSection(&g_hookLock);
    logger::info("JVM Manager encerrado.");
}

JavaVM* getJavaVM() { return g_vm; }
jvmtiEnv* getJvmtiEnv() { return g_jvmti; }
jobject getGameClassLoader() { return g_gameLoader; }

void setHookTargets(const std::vector<std::string>& targets) {
    ScopedLock lock(&g_hookLock);
    g_hookTargets.clear();
    for (const auto& t : targets) {
        g_hookTargets.insert(t);
    }
    logger::info("Hook targets atualizados (%zu alvos)", g_hookTargets.size());
}

bool isHookTarget(const std::string& name) {
    ScopedLock lock(&g_hookLock);
    return g_hookTargets.find(name) != g_hookTargets.end();
}

int retransformClass(JNIEnv* env, jclass clazz) {
    if (!g_jvmti || !clazz) return -1;
    jvmtiError err = g_jvmti->RetransformClasses(1, &clazz);
    if (err != JVMTI_ERROR_NONE) {
        logger::warn("RetransformClasses retornou codigo: %d", static_cast<int>(err));
    }
    return static_cast<int>(err);
}

static void JNICALL nSetTargets(JNIEnv* env, jclass hookClass, jobjectArray names) {
    if (!names) return;
    if (!g_hookManagerCls) {
        jmethodID dispatch = env->GetStaticMethodID(hookClass, "dispatch",
            "(Ljava/lang/ClassLoader;Ljava/lang/String;Ljava/lang/Class;Ljava/security/ProtectionDomain;[B)[B");
        if (!dispatch || env->ExceptionCheck()) return;
        jclass globalClass = reinterpret_cast<jclass>(env->NewGlobalRef(hookClass));
        if (!globalClass) return;
        g_dispatchMid = dispatch;
        g_hookManagerCls = globalClass;
        logger::info("HookManager.dispatch vinculado ao callback JVMTI.");
    }
    std::vector<std::string> targets;
    jsize n = env->GetArrayLength(names);
    for (jsize i = 0; i < n; i++) {
        jstring s = reinterpret_cast<jstring>(env->GetObjectArrayElement(names, i));
        const char* c = env->GetStringUTFChars(s, nullptr);
        if (c) {
            targets.emplace_back(c);
            env->ReleaseStringUTFChars(s, c);
        }
        env->DeleteLocalRef(s);
    }
    setHookTargets(targets);
}

static jint JNICALL nRetransform(JNIEnv* env, jclass, jclass target) {
    return retransformClass(env, target);
}

} 
} 

extern "C" {

JNIEXPORT void JNICALL Java_br_lock_injection_HookManager_nSetTargets(JNIEnv* env, jclass cls, jobjectArray names) {
    nebula::jvm::nSetTargets(env, cls, names);
}

JNIEXPORT jint JNICALL Java_br_lock_injection_HookManager_nRetransform(JNIEnv* env, jclass cls, jclass target) {
    return nebula::jvm::nRetransform(env, cls, target);
}

}

