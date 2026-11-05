#include "nebula/bridge/native_bridge.hpp"
#include "nebula/ui/state.hpp"
#include "nebula/ui/theme.hpp"
#include "nebula/ui/visual_frame.hpp"
#include <chrono>
#include <cstdio>
namespace nebula::gui {
static bool enabled(const std::vector<bridge::ModuleItem> &modules, const char *name) {
    for (const auto &module : modules)
        if (module.name == name) return module.enabled;
    return false;
}
bool hasVisualOverlays() {
    auto modules = bridge::getSharedModulesSnapshot();
    return enabled(*modules, "Nametags") || enabled(*modules, "DamageIndicator");
}
void renderVisuals() {
    auto modules = bridge::getSharedModulesSnapshot();
    if (!enabled(*modules, "Nametags") || bridge::isGuiOpen()) return;
    auto frame = sharedVisualFrame();
    if (std::chrono::duration<float>(std::chrono::steady_clock::now() - frame->projected).count() >= .6f) return;
    ImDrawList *draw = ImGui::GetForegroundDrawList();
    ImFont *font = state().body ? state().body : ImGui::GetFont();
    for (const auto &player : frame->players) {
        const auto &data = player.data;
        if (data[5] <= 0 || data[6] <= 0) continue;
        char status[80];
        std::snprintf(status, sizeof(status), "%.1f HP / %.1fm", data[0], data[2]);
        ImVec2 pos{data[3] - 70.f, data[4] - 35.f};
        draw->AddRectFilled(pos, {pos.x + 140.f, pos.y + 32.f}, IM_COL32(11, 14, 17, 225), 6.f);
        draw->AddRect(pos, {pos.x + 140.f, pos.y + 32.f}, toU32(col::BORDER), 6.f);
        draw->AddText(font, 15.f, {pos.x + 8.f, pos.y + 4.f}, toU32(col::TEXT), player.text[0].c_str());
        draw->AddText(font, 12.f, {pos.x + 8.f, pos.y + 19.f}, toU32(col::ACCENT), status);
    }
}
} 
