#pragma once

#include "nebula/core/types.hpp"
#include <cstdarg>
#include <string>

namespace nebula {
namespace logger {

void init();
void shutdown();
void log(LogLevel level, const char* fmt, ...);

inline void debug(const char* fmt, ...) {
    va_list args;
    va_start(args, fmt);
    char buf[1024];
    vsnprintf(buf, sizeof(buf), fmt, args);
    va_end(args);
    log(LogLevel::DEBUG_LEVEL, "%s", buf);
}

inline void info(const char* fmt, ...) {
    va_list args;
    va_start(args, fmt);
    char buf[1024];
    vsnprintf(buf, sizeof(buf), fmt, args);
    va_end(args);
    log(LogLevel::INFO_LEVEL, "%s", buf);
}

inline void warn(const char* fmt, ...) {
    va_list args;
    va_start(args, fmt);
    char buf[1024];
    vsnprintf(buf, sizeof(buf), fmt, args);
    va_end(args);
    log(LogLevel::WARN_LEVEL, "%s", buf);
}

inline void error(const char* fmt, ...) {
    va_list args;
    va_start(args, fmt);
    char buf[1024];
    vsnprintf(buf, sizeof(buf), fmt, args);
    va_end(args);
    log(LogLevel::ERROR_LEVEL, "%s", buf);
}

std::vector<std::string> getRecentLogs(size_t maxCount = 50);

} 
} 

