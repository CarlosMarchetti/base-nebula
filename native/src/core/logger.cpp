#include "nebula/core/logger.hpp"
#include <cstdio>
#include <ctime>
#include <deque>

namespace nebula {
namespace logger {

static CRITICAL_SECTION g_logLock;
static bool g_initialized = false;
static char g_logPath[MAX_PATH] = {0};
static std::deque<std::string> g_recentLogs;
static const size_t MAX_LOG_HISTORY = 200;

void init() {
    if (g_initialized) return;
    InitializeCriticalSection(&g_logLock);

    DWORD n = GetTempPathA(MAX_PATH, g_logPath);
    if (n > 0 && n < MAX_PATH) {
        strcat_s(g_logPath, "nebula-client.log");
        FILE* f = nullptr;
        if (fopen_s(&f, g_logPath, "w") == 0 && f) {
            fprintf(f, "=== %s Log Iniciado ===\n", NEBULA_BUILD_NAME);
            fclose(f);
        }
    }

    g_initialized = true;
}

void shutdown() {
    if (!g_initialized) return;
    DeleteCriticalSection(&g_logLock);
    g_initialized = false;
}

void log(LogLevel level, const char* fmt, ...) {
    if (!g_initialized) init();

    ScopedLock lock(&g_logLock);

    time_t now = time(nullptr);
    tm t;
    localtime_s(&t, &now);
    char timeBuf[32];
    strftime(timeBuf, sizeof(timeBuf), "%H:%M:%S", &t);

    const char* levelStr = "INFO";
    switch (level) {
        case LogLevel::DEBUG_LEVEL: levelStr = "DEBUG"; break;
        case LogLevel::INFO_LEVEL:  levelStr = "INFO";  break;
        case LogLevel::WARN_LEVEL:  levelStr = "WARN";  break;
        case LogLevel::ERROR_LEVEL: levelStr = "ERROR"; break;
    }

    va_list args;
    va_start(args, fmt);
    char msgBuf[2048];
    vsnprintf(msgBuf, sizeof(msgBuf), fmt, args);
    va_end(args);

    char fullLine[2560];
    snprintf(fullLine, sizeof(fullLine), "[%s] [%s] %s", timeBuf, levelStr, msgBuf);

    if (g_recentLogs.size() >= MAX_LOG_HISTORY) {
        g_recentLogs.pop_front();
    }
    g_recentLogs.push_back(fullLine);

    if (g_logPath[0] != '\0') {
        FILE* f = nullptr;
        if (fopen_s(&f, g_logPath, "a") == 0 && f) {
            fprintf(f, "%s\n", fullLine);
            fclose(f);
        }
    }

    OutputDebugStringA(fullLine);
    OutputDebugStringA("\n");
}

std::vector<std::string> getRecentLogs(size_t maxCount) {
    if (!g_initialized) return {};
    ScopedLock lock(&g_logLock);
    size_t count = (maxCount < g_recentLogs.size()) ? maxCount : g_recentLogs.size();
    std::vector<std::string> result;
    result.reserve(count);
    size_t startIdx = g_recentLogs.size() - count;
    for (size_t i = startIdx; i < g_recentLogs.size(); ++i) {
        result.push_back(g_recentLogs[i]);
    }
    return result;
}

} 
} 

