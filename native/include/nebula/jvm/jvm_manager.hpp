#pragma once

#include <windows.h>
#include <jni.h>
#include <jvmti.h>
#include <string>
#include <set>
#include <vector>

namespace nebula {
namespace jvm {

class ScopedJniEnv {
public:
    ScopedJniEnv();
    ~ScopedJniEnv();
    ScopedJniEnv(const ScopedJniEnv&)=delete;
    ScopedJniEnv& operator=(const ScopedJniEnv&)=delete;

    JNIEnv* get() const { return m_env; }
    JNIEnv* operator->() const { return m_env; }
    bool isValid() const { return m_env != nullptr; }

private:
    JNIEnv* m_env = nullptr;
    bool m_attached = false;
};

bool init();
void shutdown();

JavaVM* getJavaVM();
jvmtiEnv* getJvmtiEnv();
jobject getGameClassLoader();

bool clearException(JNIEnv* env, const char* context = "");
bool isExceptionOfType(JNIEnv* env, const char* className);

void setHookTargets(const std::vector<std::string>& targets);
bool isHookTarget(const std::string& name);
int retransformClass(JNIEnv* env, jclass clazz);

} 
} 

extern "C" {
JNIEXPORT void JNICALL Java_br_lock_injection_HookManager_nSetTargets(JNIEnv* env, jclass cls, jobjectArray names);
JNIEXPORT jint JNICALL Java_br_lock_injection_HookManager_nRetransform(JNIEnv* env, jclass cls, jclass target);
}
