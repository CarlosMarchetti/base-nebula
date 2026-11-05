#pragma once

#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>
#include <string>
#include <vector>
#include <memory>
#include <cstdint>

#include "nebula/core/version.hpp"
#define NEBULA_BUILD_NAME "Nebula Client (Lunar 1.8.9)"

namespace nebula {

enum class LogLevel {
    DEBUG_LEVEL,
    INFO_LEVEL,
    WARN_LEVEL,
    ERROR_LEVEL
};

struct ScopedLock {
    CRITICAL_SECTION* cs;
    ScopedLock(CRITICAL_SECTION* cs_) : cs(cs_) {
        EnterCriticalSection(cs);
    }
    ScopedLock(const ScopedLock&)=delete;
    ScopedLock& operator=(const ScopedLock&)=delete;
    ~ScopedLock() {
        LeaveCriticalSection(cs);
    }
};

} 

