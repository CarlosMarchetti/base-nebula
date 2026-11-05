#pragma once

#include <vector>
#include <string>
#include <cstdint>
#include <jni.h>

namespace nebula {
namespace jvm {

bool loadAndInjectPayload(JNIEnv* env, jobject gameLoader,
                          const uint8_t* rawData, size_t rawSize,
                          const uint8_t key[32]);

} 
} 

