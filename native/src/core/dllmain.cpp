#include <windows.h>
#include "nebula/core/types.hpp"
#include "nebula/core/logger.hpp"
#include "nebula/security/crypto.hpp"
#include "nebula/jvm/jvm_manager.hpp"
#include "nebula/jvm/payload_loader.hpp"
#include "nebula/bridge/native_bridge.hpp"
#include "nebula/render/render_hook.hpp"

extern "C" const unsigned char g_payload[];
extern "C" const unsigned char g_payload_end[];

static const uint8_t g_masterKey[32] = {
    0x53, 0x74, 0x61, 0x72, 0x43, 0x6c, 0x69, 0x65,
    0x6e, 0x74, 0x2d, 0x41, 0x70, 0x6f, 0x6c, 0x6f,
    0x2d, 0x4b, 0x65, 0x79, 0x2d, 0x32, 0x30, 0x32,
    0x36, 0x2d, 0x4c, 0x75, 0x6e, 0x61, 0x72, 0x38
};

namespace nebula {

static DWORD WINAPI mainThread(LPVOID) {
    logger::init();
    logger::info("=========================================");
    logger::info("Iniciando %s", NEBULA_BUILD_NAME);
    logger::info("=========================================");

    bridge::init();

    if (!jvm::init()) {
        logger::error("Falha ao inicializar subsistema JVM/JVMTI!");
        return 1;
    }

    if (!render::init()) {
        logger::warn("Aviso: Hook de renderizacao OpenGL falhou ou adiado.");
    }

    jvm::ScopedJniEnv env;
    if (!env.isValid()) {
        logger::error("Falha ao obter JNIEnv para injecao do payload!");
        return 1;
    }

    size_t payloadSize = static_cast<size_t>(g_payload_end - g_payload);
    logger::info("Tamanho do payload bruto na DLL: %zu bytes", payloadSize);

    if (!jvm::loadAndInjectPayload(env.get(), jvm::getGameClassLoader(),
                                   g_payload, payloadSize, g_masterKey)) {
        logger::error("Falha critica na injecao das classes Java!");
        return 1;
    }

    logger::info("Nebula injetado e ativo no processo do jogo.");
    return 0;
}

} 

BOOL WINAPI DllMain(HINSTANCE hinst, DWORD reason, LPVOID reserved) {
    if (reason == DLL_PROCESS_ATTACH) {
        DisableThreadLibraryCalls(hinst);
        HANDLE h = CreateThread(nullptr, 0, nebula::mainThread, nullptr, 0, nullptr);
        if (h) CloseHandle(h);
    } else if (reason == DLL_PROCESS_DETACH) {
        if (!reserved) { 
            nebula::render::shutdown();
            nebula::bridge::shutdown();
            nebula::jvm::shutdown();
            nebula::logger::shutdown();
        }
    }
    return TRUE;
}

