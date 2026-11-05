#include "nebula/jvm/global_refs.hpp"
#include "nebula/jvm/payload_loader.hpp"
#include "nebula/jvm/jvm_manager.hpp"
#include "nebula/security/crypto.hpp"
#include "nebula/core/logger.hpp"
#include "nebula/bridge/native_bridge.hpp"
#include <map>
#include <cstring>
#include <algorithm>

namespace nebula {
namespace jvm {

struct Entry {
    std::string name;
    const uint8_t* data;
    uint32_t size;
};

static bool endsWith(const std::string& s, const char* suffix) {
    size_t n = strlen(suffix);
    return s.size() >= n && s.compare(s.size() - n, n, suffix) == 0;
}

static bool startsWith(const std::string& s, const char* prefix) {
    return s.rfind(prefix, 0) == 0;
}

static bool shouldDefine(const std::string& internalName) {
    return startsWith(internalName, "dev/nebula/client/");
}

static jclass tryLoadExisting(JNIEnv* env, jobject loader, const std::string& internalName) {
    jclass loaderCls  = env->FindClass("java/lang/ClassLoader");
    jmethodID load    = env->GetMethodID(loaderCls, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
    std::string dotName = internalName;
    std::replace(dotName.begin(), dotName.end(), '/', '.');
    jstring jname = env->NewStringUTF(dotName.c_str());
    jclass cls = reinterpret_cast<jclass>(env->CallObjectMethod(loader, load, jname));
    env->DeleteLocalRef(jname);
    env->DeleteLocalRef(loaderCls);
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return nullptr;
    }
    return cls;
}

static std::vector<Entry> parseEntries(const uint8_t* buf, size_t len) {
    std::vector<Entry> out;
    const uint8_t* p = buf;
    const uint8_t* end = buf + len;

    auto u32 = [&]() -> uint32_t {
        if (p + 4 > end) return 0;
        uint32_t v;
        memcpy(&v, p, 4);
        p += 4;
        return v;
    };

    uint32_t count = u32();
    for (uint32_t i = 0; i < count && p < end; i++) {
        uint32_t nameLen = u32();
        if (p + nameLen > end) break;
        std::string name(reinterpret_cast<const char*>(p), nameLen);
        p += nameLen;
        uint32_t size = u32();
        if (p + size > end) break;
        out.push_back({name, p, size});
        p += size;
    }
    return out;
}

static std::map<std::string, jclass> defineClasses(JNIEnv* env, jobject loader, const std::vector<Entry>& entries) {
    std::map<std::string, jclass> defined;
    std::vector<const Entry*> pending;

    for (const Entry& e : entries) {
        if (!endsWith(e.name, ".class")) continue;
        std::string internal = e.name.substr(0, e.name.size() - 6);
        if (!shouldDefine(internal)) {
            jclass cls = tryLoadExisting(env, loader, internal);
            if (cls) {
                defined[internal] = reinterpret_cast<jclass>(env->NewGlobalRef(cls));
                env->DeleteLocalRef(cls);
            }
            continue;
        }
        pending.push_back(&e);
    }

    bool progress = true;
    while (!pending.empty() && progress) {
        progress = false;
        std::vector<const Entry*> next;

        for (const Entry* e : pending) {
            std::string internal = e->name.substr(0, e->name.size() - 6);
            jclass cls = env->DefineClass(internal.c_str(), loader,
                                          reinterpret_cast<const jbyte*>(e->data), static_cast<jsize>(e->size));
            if (cls) {
                defined[internal] = reinterpret_cast<jclass>(env->NewGlobalRef(cls));
                env->DeleteLocalRef(cls);
                progress = true;
                continue;
            }

            if (env->ExceptionCheck()) {
                jthrowable ex = env->ExceptionOccurred();
                env->ExceptionClear();
                jclass linkageErr = env->FindClass("java/lang/LinkageError");
                bool isLinkage = linkageErr && env->IsInstanceOf(ex, linkageErr);
                if (linkageErr) env->DeleteLocalRef(linkageErr);
                env->DeleteLocalRef(ex);

                if (isLinkage) {
                    jclass existing = tryLoadExisting(env, loader, internal);
                    if (existing) {
                        defined[internal] = reinterpret_cast<jclass>(env->NewGlobalRef(existing));
                        env->DeleteLocalRef(existing);
                        progress = true;
                        continue;
                    }
                }
            }

            next.push_back(e);
        }
        pending.swap(next);
    }

    for (const Entry* e : pending) {
        logger::warn("Nao foi possivel definir classe: %s", e->name.c_str());
    }

    return defined;
}

static void pushResources(JNIEnv* env, jclass resourcesCls, const std::vector<Entry>& entries) {
    jmethodID putMid = env->GetStaticMethodID(resourcesCls, "put", "(Ljava/lang/String;[B)V");
    jmethodID regMid = env->GetStaticMethodID(resourcesCls, "registerClass", "(Ljava/lang/String;)V");
    if (!putMid || !regMid) {
        clearException(env, "Resources methods");
        return;
    }

    for (const Entry& e : entries) {
        if (endsWith(e.name, ".class")) {
            std::string internal = e.name.substr(0, e.name.size() - 6);
            jstring jname = env->NewStringUTF(internal.c_str());
            env->CallStaticVoidMethod(resourcesCls, regMid, jname);
            clearException(env, "registerClass");
            env->DeleteLocalRef(jname);
        } else {
            jstring jname = env->NewStringUTF(e.name.c_str());
            jbyteArray data = env->NewByteArray(static_cast<jsize>(e.size));
            env->SetByteArrayRegion(data, 0, static_cast<jsize>(e.size), reinterpret_cast<const jbyte*>(e.data));
            env->CallStaticVoidMethod(resourcesCls, putMid, jname, data);
            clearException(env, "putResource");
            env->DeleteLocalRef(jname);
            env->DeleteLocalRef(data);
        }
    }
}

bool loadAndInjectPayload(JNIEnv* env, jobject gameLoader,
                          const uint8_t* rawData, size_t rawSize,
                          const uint8_t key[32]) {
    logger::info("Iniciando decriptacao e carregamento do payload protegido...");

    std::vector<uint8_t> decrypted;
    if (!security::decryptPayload(rawData, rawSize, key, decrypted)) {
        logger::error("Falha ao decifrar ou validar integridade (SHA-256) do payload!");
        return false;
    }

    logger::info("Payload autenticado e decifrado com sucesso (%zu bytes).", decrypted.size());

    std::vector<Entry> entries = parseEntries(decrypted.data(), decrypted.size());
    logger::info("Total de entradas no payload: %zu", entries.size());

    std::map<std::string, jclass> defined = defineClasses(env, gameLoader, entries);
    GlobalClassRefs ownedRefs(env,defined);
    logger::info("Classes definidas com sucesso: %zu", defined.size());

    auto findCls = [&](const char* name) -> jclass {
        auto it = defined.find(name);
        return (it != defined.end()) ? it->second : nullptr;
    };

    jclass resourcesCls = findCls("dev/nebula/client/injection/Resources");
    jclass hookCls      = findCls("dev/nebula/client/injection/HookManager");
    jclass bootstrapCls = findCls("dev/nebula/client/Bootstrap");
    jclass bridgeCls    = findCls("dev/nebula/client/bridge/NativeBridge");

    if (!resourcesCls || !hookCls || !bootstrapCls) {
        logger::error("Classes obrigatorias de bootstrap ausentes no payload!");
        security::secureWipe(decrypted.data(), decrypted.size());
        return false;
    }

    if (bridgeCls) {
        bridge::bindJavaBridge(env, bridgeCls);
    } else {
        logger::warn("Classe dev/nebula/client/bridge/NativeBridge nao encontrada no payload!");
    }

    pushResources(env, resourcesCls, entries);

    JNINativeMethod hookNatives[] = {
        {const_cast<char*>("nSetTargets"),  const_cast<char*>("([Ljava/lang/String;)V"), reinterpret_cast<void*>(Java_br_lock_injection_HookManager_nSetTargets)},
        {const_cast<char*>("nRetransform"), const_cast<char*>("(Ljava/lang/Class;)I"),   reinterpret_cast<void*>(Java_br_lock_injection_HookManager_nRetransform)}
    };
    if (env->RegisterNatives(hookCls, hookNatives, 2) != JNI_OK) {
        clearException(env, "RegisterNatives HookManager");
    }

    security::secureWipe(decrypted.data(), decrypted.size());
    decrypted.clear();
    logger::info("Buffer de memoria do bytecode decifrado foi limpo com sucesso (SecureZeroMemory).");

    jmethodID startMid = env->GetStaticMethodID(bootstrapCls, "start", "()V");
    if (!startMid) {
        clearException(env, "Bootstrap.start mid");
        return false;
    }

    logger::info("Chamando dev.nebula.client.Bootstrap.start()...");
    env->CallStaticVoidMethod(bootstrapCls, startMid);
    clearException(env, "Bootstrap.start");

    logger::info("Bootstrap iniciado com sucesso!");
    return true;
}

} 
} 

