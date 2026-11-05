#include "nebula/render/render_hook.hpp"
#include "backends/imgui_impl_opengl2.h"
#include "backends/imgui_impl_win32.h"
#include "imgui.h"
#include "nebula/bridge/native_bridge.hpp"
#include "nebula/core/logger.hpp"
#include "nebula/render/input_gate.hpp"
#include "nebula/ui/nebula_gui.hpp"
#include "nebula/ui/visual_frame.hpp"
#include <GL/gl.h>
#include <MinHook.h>
#include <cstring>
#include <string>

extern IMGUI_IMPL_API LRESULT ImGui_ImplWin32_WndProcHandler(HWND hWnd, UINT msg, WPARAM wParam,
                                                             LPARAM lParam);

namespace nebula {
namespace render {

typedef BOOL(WINAPI *wglSwapBuffers_t)(HDC);
static wglSwapBuffers_t g_originalWglSwapBuffers = nullptr;

static HWND g_gameHwnd = nullptr;
static WNDPROC g_originalWndProc = nullptr;
static bool g_imguiInitialized = false;
static BOOL WINAPI hookedWglSwapBuffers(HDC hdc);

static ImFont *loadFontBesideModule(const char *fileName, float pixelSize) {
    HMODULE module = nullptr;
    const auto address = reinterpret_cast<LPCSTR>(&hookedWglSwapBuffers);
    if (!GetModuleHandleExA(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS |
                                GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT,
                            address, &module))
        return nullptr;

    char modulePath[MAX_PATH] = {};
    if (!GetModuleFileNameA(module, modulePath, MAX_PATH))
        return nullptr;
    char *slash = std::strrchr(modulePath, '\\');
    if (!slash)
        return nullptr;
    *(slash + 1) = '\0';

    const std::string path = std::string(modulePath) + "fonts\\" + fileName;
    return ImGui::GetIO().Fonts->AddFontFromFileTTF(path.c_str(), pixelSize, nullptr,
                                                    ImGui::GetIO().Fonts->GetGlyphRangesDefault());
}

static LRESULT CALLBACK hookedWndProc(HWND hwnd, UINT uMsg, WPARAM wParam, LPARAM lParam) {
    if (uMsg == WM_KEYDOWN) {
        bool isRepeat = (lParam & (1 << 30)) != 0;

        if (wParam == VK_INSERT && !isRepeat) {
            bridge::toggleGui();
            return 0;
        }
        if (wParam == VK_ESCAPE && bridge::isGuiOpen()) {
            bridge::setGuiOpen(false);
            return 0;
        }
    }

    if (bridge::isGuiOpen()) {
        ImGui_ImplWin32_WndProcHandler(hwnd, uMsg, wParam, lParam);
        if (uMsg == WM_INPUT)
            return DefWindowProc(hwnd, uMsg, wParam, lParam);
        if (blockedGameInput(uMsg))
            return 0;
    }

    return CallWindowProc(g_originalWndProc, hwnd, uMsg, wParam, lParam);
}

static BOOL WINAPI hookedWglSwapBuffers(HDC hdc) {
    if (!g_imguiInitialized) {
        g_gameHwnd = WindowFromDC(hdc);
        if (g_gameHwnd) {
            IMGUI_CHECKVERSION();
            ImGui::CreateContext();

            ImGuiIO &io = ImGui::GetIO();
            io.ConfigFlags |= ImGuiConfigFlags_NavEnableKeyboard;
            io.IniFilename = nullptr; 

            ImFont *bodyFont = loadFontBesideModule("Sansation-Regular.ttf", 17.f);
            ImFont *displayFont = loadFontBesideModule("Sansation-Bold.ttf", 29.f);
            if (bodyFont)
                io.FontDefault = bodyFont;
            else
                bodyFont = io.Fonts->AddFontDefault();
            if (!displayFont)
                displayFont = bodyFont;
            gui::setFonts(bodyFont, displayFont);

            gui::initTheme();

            ImGui_ImplWin32_Init(g_gameHwnd);
            ImGui_ImplOpenGL2_Init();

            g_originalWndProc = reinterpret_cast<WNDPROC>(
                SetWindowLongPtrA(g_gameHwnd, GWLP_WNDPROC, reinterpret_cast<LONG_PTR>(hookedWndProc)));

            g_imguiInitialized = true;
            logger::info("Dear ImGui e Hook de Apresentacao OpenGL inicializados com sucesso.");
        }
    }

    if (g_imguiInitialized) {
        ImGuiIO &io = ImGui::GetIO();
        io.MouseDrawCursor = bridge::isGuiOpen();
    }

    if (g_imguiInitialized && (bridge::isGuiOpen() || bridge::isHudEnabled() || gui::hasVisualOverlays())) {
        ImGui_ImplOpenGL2_NewFrame();
        ImGui_ImplWin32_NewFrame();
        ImGui::NewFrame();

        if (bridge::isHudEnabled()) {
            gui::renderHud();
        }
        gui::renderNotifications();
        gui::renderDamageIndicator();
        gui::renderVisuals();

        if (bridge::isGuiOpen()) {
            gui::renderGui();
        }

        ImGui::Render();
        ImGui_ImplOpenGL2_RenderDrawData(ImGui::GetDrawData());
    }

    return g_originalWglSwapBuffers(hdc);
}

bool init() {
    if (MH_Initialize() != MH_OK) {
        logger::error("MH_Initialize falhou");
        return false;
    }

    HMODULE opengl32 = GetModuleHandleA("opengl32.dll");
    if (!opengl32) {
        opengl32 = LoadLibraryA("opengl32.dll");
    }
    if (!opengl32) {
        logger::error("opengl32.dll nao encontrada");
        return false;
    }

    void *targetSwap = reinterpret_cast<void *>(GetProcAddress(opengl32, "wglSwapBuffers"));
    if (!targetSwap) {
        logger::error("wglSwapBuffers nao encontrado em opengl32.dll");
        return false;
    }

    if (MH_CreateHook(targetSwap, reinterpret_cast<void *>(hookedWglSwapBuffers),
                      reinterpret_cast<void **>(&g_originalWglSwapBuffers)) != MH_OK) {
        logger::error("MH_CreateHook para wglSwapBuffers falhou");
        return false;
    }

    if (MH_EnableHook(targetSwap) != MH_OK) {
        logger::error("MH_EnableHook para wglSwapBuffers falhou");
        return false;
    }

    logger::info("Hook de wglSwapBuffers instalado.");
    return true;
}

void shutdown() {
    if (g_originalWndProc && g_gameHwnd) {
        SetWindowLongPtrA(g_gameHwnd, GWLP_WNDPROC, reinterpret_cast<LONG_PTR>(g_originalWndProc));
        g_originalWndProc = nullptr;
    }

    if (g_imguiInitialized) {
        ImGui_ImplOpenGL2_Shutdown();
        ImGui_ImplWin32_Shutdown();
        ImGui::DestroyContext();
        g_imguiInitialized = false;
    }

    MH_DisableHook(MH_ALL_HOOKS);
    MH_Uninitialize();
    logger::info("Render Hook encerrado.");
}

HWND getGameWindow() {
    return g_gameHwnd;
}

} 
} 
