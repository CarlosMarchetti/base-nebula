#include "nebula/bridge/native_bridge.hpp"
#include "nebula/ui/nebula_gui.hpp"
#include "nebula/ui/state.hpp"
#include "nebula/ui/theme.hpp"
#include "nebula/ui/visual_frame.hpp"
#include <algorithm>
#include <cmath>
#include <cstdlib>
namespace nebula::gui {
using bridge::DamageSnapshot;
using bridge::ModuleItem;
using bridge::SettingItem;
void renderHud() {
    bool guiOpen = bridge::isGuiOpen();
    auto sharedModules = bridge::getSharedModulesSnapshot();
    const auto &g_modSnapshot = *sharedModules;

    ImGuiWindowFlags flags = ImGuiWindowFlags_AlwaysAutoResize | ImGuiWindowFlags_NoCollapse;
    if (!guiOpen) {
        flags |= ImGuiWindowFlags_NoInputs | ImGuiWindowFlags_NoDecoration | ImGuiWindowFlags_NoBackground;
    }

    ImGui::SetNextWindowPos({16.f, 16.f}, ImGuiCond_FirstUseEver);

    if (guiOpen) {
        ImGui::PushStyleColor(ImGuiCol_WindowBg, {col::BG.x, col::BG.y, col::BG.z, 0.94f});
        ImGui::PushStyleColor(ImGuiCol_Border, col::ACCENT);
        ImGui::PushStyleVar(ImGuiStyleVar_WindowBorderSize, 1.0f);
        ImGui::PushStyleVar(ImGuiStyleVar_WindowPadding, {10.f, 8.f});
    }

    bool hudWindowOpen = true;
    if (ImGui::Begin("##nebula_hud", &hudWindowOpen, flags)) {
        ImGui::PushFont(state().body ? state().body : ImGui::GetFont());
        ImDrawList *dl = ImGui::GetWindowDrawList();
        const ModuleItem *config = nullptr;
        for (const auto &module : g_modSnapshot) {
            if (module.name == "ArrayList") {
                config = &module;
                break;
            }
        }
        auto setting = [&](const std::string &name) -> const SettingItem * {
            if (!config)
                return nullptr;
            for (const auto &item : config->settings)
                if (item.name == name)
                    return &item;
            return nullptr;
        };
        const SettingItem *modeSetting = setting("Mode");
        const SettingItem *watermarkSetting = setting("Watermark");
        const SettingItem *spacedSetting = setting("Spaced Modules");
        const SettingItem *waveSetting = setting("Wave");
        const SettingItem *primarySetting = setting("Primary Color");
        const SettingItem *secondarySetting = setting("Secondary Color");
        const std::string mode = modeSetting ? modeSetting->value : "Default";
        const bool watermark = !watermarkSetting || watermarkSetting->boolVal;
        const bool spaced = spacedSetting && spacedSetting->boolVal;
        const bool wave = waveSetting && waveSetting->boolVal;
        auto unpackColor = [](const SettingItem *item, uint32_t fallback) {
            const uint32_t value =
                item ? static_cast<uint32_t>(std::strtoll(item->value.c_str(), nullptr, 10)) : fallback;
            return ImVec4(((value >> 16) & 0xff) / 255.f, ((value >> 8) & 0xff) / 255.f,
                          (value & 0xff) / 255.f, ((value >> 24) & 0xff) / 255.f);
        };
        const ImVec4 primary = unpackColor(primarySetting, 0xFFEB4567u);
        const ImVec4 secondary = unpackColor(secondarySetting, 0xFF4AD4E8u);

        if (watermark) {
            if (state().display)
                ImGui::PushFont(state().display);
            ImGui::PushStyleColor(ImGuiCol_Text, col::ACCENT);
            ImGui::Text("NEBULA");
            ImGui::PopStyleColor();
            if (state().display)
                ImGui::PopFont();
            ImGui::SameLine();
            ImGui::TextColored(col::CYAN, "1.8.9");
            if (guiOpen) {
                ImGui::SameLine();
                ImGui::TextDisabled(" [Arraste]");
                ImGui::Separator();
            }
            ImGui::Spacing();
        }

        std::vector<ModuleItem> visible;
        for (const auto &mod : g_modSnapshot) {
            if (mod.enabled && mod.name != "ArrayList" && mod.name != "Notifications" &&
                mod.name != "ClickGui")
                visible.push_back(mod);
        }
        std::stable_sort(visible.begin(), visible.end(), [](const ModuleItem &a, const ModuleItem &b) {
            return ImGui::CalcTextSize(a.name.c_str()).x > ImGui::CalcTextSize(b.name.c_str()).x;
        });
        int activeCount = 0;
        for (const auto &mod : visible) {
            activeCount++;
            ImVec2 mpos = ImGui::GetCursorScreenPos();
            ImVec4 tone = primary;
            if (wave) {
                const float phase = ImGui::GetTime() * 1.6f + activeCount * 0.42f;
                const float blend = (std::sin(phase) + 1.f) * 0.5f;
                tone.x = primary.x + (secondary.x - primary.x) * blend;
                tone.y = primary.y + (secondary.y - primary.y) * blend;
                tone.z = primary.z + (secondary.z - primary.z) * blend;
            }

            const float rowHeight = 18.f;
            if (mode == "Rise") {
                dl->AddRectFilled(
                    {mpos.x, mpos.y},
                    {mpos.x + ImGui::CalcTextSize(mod.name.c_str()).x + 19.f, mpos.y + rowHeight},
                    IM_COL32(15, 16, 22, 224), 4.f);
                dl->AddRectFilled({mpos.x, mpos.y}, {mpos.x + 2.f, mpos.y + rowHeight}, toU32(tone), 2.f);
            } else if (mode == "Tesseract") {
                dl->AddRect({mpos.x, mpos.y},
                            {mpos.x + ImGui::CalcTextSize(mod.name.c_str()).x + 16.f, mpos.y + rowHeight},
                            toU32(tone), 2.f, 0, 1.f);
            } else if (mode == "VapeV4") {
                dl->AddRectFilled(
                    {mpos.x, mpos.y},
                    {mpos.x + ImGui::CalcTextSize(mod.name.c_str()).x + 20.f, mpos.y + rowHeight},
                    IM_COL32(10, 10, 12, 235), 6.f);
            }

            dl->AddCircleFilled({mpos.x + 7.f, mpos.y + 9.f}, 2.5f, toU32(tone));
            ImGui::SetCursorPosX(ImGui::GetCursorPosX() + 14.f);
            ImGui::PushStyleColor(ImGuiCol_Text, mode == "Default" ? col::TEXT : tone);
            ImGui::Text("%s", mod.name.c_str());
            ImGui::PopStyleColor();
            if (spaced)
                ImGui::SetCursorPosY(ImGui::GetCursorPosY() + 4.f);
        }

        if (activeCount == 0 && guiOpen) {
            ImGui::TextDisabled("Nenhum modulo ativo");
        }
        ImGui::PopFont();
    }
    ImGui::End();

    if (guiOpen) {
        ImGui::PopStyleVar(2);
        ImGui::PopStyleColor(2);
    }
}

void renderNotifications() {
    const auto notifications = bridge::getNotificationsSnapshot();
    if (notifications.empty())
        return;

    ImDrawList *draw = ImGui::GetForegroundDrawList();
    const ImVec2 display = ImGui::GetIO().DisplaySize;
    float bottom = display.y - 20.f;
    const auto now = std::chrono::steady_clock::now();

    for (auto it = notifications.rbegin(); it != notifications.rend(); ++it) {
        const float age = std::chrono::duration<float>(now - it->createdAt).count();
        const float alpha = age > 3.5f ? std::max(0.f, (4.2f - age) / 0.7f) : 1.f;
        if (alpha <= 0.f)
            continue;

        const float textWidth = 300.f;
        const ImVec2 body = ImGui::CalcTextSize(it->message.c_str(), nullptr, false, textWidth);
        const float width = std::min(360.f, std::max(190.f, body.x + 34.f));
        const float height = body.y + 38.f;
        const float x = display.x - width - 18.f;
        const float y = bottom - height;
        ImVec4 tone = col::CYAN;
        if (it->type == "ERROR")
            tone = {0.96f, 0.29f, 0.34f, 1.f};
        else if (it->type == "SUCCESS" || it->type == "ENABLED")
            tone = col::GREEN;
        else if (it->type == "DISABLED")
            tone = col::TEXT_DIM;

        ImVec4 panel = col::CARD;
        panel.w = 0.96f * alpha;
        tone.w *= alpha;
        const ImVec2 min{x, y};
        const ImVec2 max{x + width, y + height};
        draw->AddRectFilled(min, max, toU32(panel), 8.f);
        draw->AddRect(min, max, IM_COL32(255, 255, 255, static_cast<int>(18.f * alpha)), 8.f);
        draw->AddRectFilled(min, {x + 3.f, y + height}, toU32(tone), 8.f, ImDrawFlags_RoundCornersLeft);
        draw->AddText({x + 13.f, y + 7.f}, toU32(tone), it->type.c_str());
        draw->AddText(ImGui::GetFont(), ImGui::GetFontSize(), {x + 13.f, y + 22.f},
                      IM_COL32(240, 241, 246, static_cast<int>(255.f * alpha)), it->message.c_str(), nullptr,
                      textWidth);
        bottom = y - 9.f;
    }
}

void renderDamageIndicator() {
    const DamageSnapshot target = bridge::getDamageSnapshot();
    static std::string lastTarget;
    static float animatedHealth = -1.f;
    if (!target.visible) {
        lastTarget.clear();
        animatedHealth = -1.f;
        return;
    }
    auto sharedFrame = sharedVisualFrame();
    const auto &frame = *sharedFrame;
    const VisualPlayer *info = nullptr;
    for (const auto &player : frame.players)
        if (player.data[34] > 0 || player.text[0] == target.name) {
            info = &player;
            break;
        }
    const float maxHealth = info && info->data[1] > 0 ? info->data[1] : 20.f;
    auto sharedModules = bridge::getSharedModulesSnapshot();
    const auto &modules = *sharedModules;
    const ModuleItem *config = nullptr;
    for (const auto &module : modules) {
        if (module.name == "DamageIndicator") {
            config = &module;
            break;
        }
    }
    if (!config || !config->enabled)
        return;

    auto setting = [&](const std::string &name) -> const SettingItem * {
        for (const auto &item : config->settings)
            if (item.name == name)
                return &item;
        return nullptr;
    };
    const SettingItem *modeSetting = setting("Mode");
    const SettingItem *scaleSetting = setting("Scale");
    const SettingItem *xSetting = setting("X");
    const SettingItem *ySetting = setting("Y");
    const std::string mode = modeSetting ? modeSetting->value : "J3";
    const float scale = scaleSetting ? static_cast<float>(scaleSetting->doubleVal) : 1.f;
    const float anchorX = xSetting ? static_cast<float>(xSetting->doubleVal) : 0.75f;
    const float anchorY = ySetting ? static_cast<float>(ySetting->doubleVal) : 0.45f;
    const ImVec2 display = ImGui::GetIO().DisplaySize;
    const bool extended = mode == "J3 Ultimate" || mode == "Astralis";
    const float width = (extended ? 230.f : 190.f) * scale;
    const float height = (extended ? 88.f : 62.f) * scale;
    const float x = std::max(8.f, std::min(display.x - width - 8.f, display.x * anchorX - width));
    const float y = std::max(8.f, std::min(display.y - height - 8.f, display.y * anchorY - height * 0.5f));
    const ImVec2 min{x, y};
    const ImVec2 max{x + width, y + height};

    ImVec4 accent = col::ACCENT;
    if (mode == "Ultimate")
        accent = {0.66f, 0.48f, 1.f, 1.f};
    else if (mode == "Astralis") {
        const SettingItem *color = setting("Color");
        uint32_t n =
            color ? static_cast<uint32_t>(std::strtoll(color->value.c_str(), nullptr, 10)) : 0xFFF28D27u;
        accent = {((n >> 16) & 255) / 255.f, ((n >> 8) & 255) / 255.f, (n & 255) / 255.f,
                  ((n >> 24) & 255) / 255.f};
    }
    const ImU32 accentU32 = toU32(accent);
    ImDrawList *draw = ImGui::GetForegroundDrawList();
    const float rounding = mode == "Astralis" ? 12.f * scale : mode == "Ultimate" ? 7.f * scale : 2.f * scale;
    draw->AddRectFilled(min, max, IM_COL32(13, 14, 19, 238), rounding);
    draw->AddRect(min, max, IM_COL32(255, 255, 255, 24), rounding, 0, 1.f);
    if (mode == "Ultimate")
        draw->AddRectFilled(min, {x + width, y + 3.f * scale}, accentU32, rounding);
    else
        draw->AddRectFilled(min, {x + 3.f * scale, y + height}, accentU32, rounding);

    if (lastTarget != target.name) {
        lastTarget = target.name;
        animatedHealth = target.health;
    }
    const float dt = std::min(ImGui::GetIO().DeltaTime, 0.1f);
    if (animatedHealth < 0.f)
        animatedHealth = target.health;
    animatedHealth += (target.health - animatedHealth) * std::min(1.f, dt * 12.f);
    const float ratio = std::max(0.f, std::min(1.f, animatedHealth / maxHealth));
    const float pad = 13.f * scale;
    const float textSize = ImGui::GetFontSize() * scale;
    const char *label = mode == "J3" ? "TARGET" : mode == "Astralis" ? "ASTRALIS" : "PLAYER INFO";
    draw->AddText(ImGui::GetFont(), textSize * 0.72f, {x + pad, y + 7.f * scale}, accentU32, label);
    draw->AddText(ImGui::GetFont(), textSize, {x + pad + (extended ? 42.f * scale : 0), y + 23.f * scale},
                  IM_COL32(245, 246, 250, 255), target.name.c_str());
    char healthText[32];
    snprintf(healthText, sizeof(healthText), "%.1f HP", target.health);
    draw->AddText(ImGui::GetFont(), textSize * 0.72f, {x + width - 54.f * scale, y + 24.f * scale},
                  IM_COL32(205, 208, 218, 255), healthText);
    if (extended && info) {
        const auto &d = info->data;
        if (d[35] > 0)
            draw->AddImage(reinterpret_cast<ImTextureID>(static_cast<intptr_t>(d[35])),
                           {x + pad, y + 24 * scale}, {x + pad + 32 * scale, y + 56 * scale}, {d[36], d[37]},
                           {d[38], d[39]});
        for (int i = 0; i < 4; i++) {
            int o = 10 + i * 6;
            if (d[o + 1] > 0) {
                float ax = x + pad + 42 * scale + i * 22 * scale;
                draw->AddImage(reinterpret_cast<ImTextureID>(static_cast<intptr_t>(d[o + 1])),
                               {ax, y + 43 * scale}, {ax + 18 * scale, y + 61 * scale}, {d[o + 2], d[o + 3]},
                               {d[o + 4], d[o + 5]});
            }
        }
    }
    const ImVec2 barMin{x + pad, y + height - 14.f * scale};
    const ImVec2 barMax{x + width - pad, y + height - 8.f * scale};
    draw->AddRectFilled(barMin, barMax, IM_COL32(48, 50, 59, 255), 3.f * scale);
    draw->AddRectFilled(barMin, {barMin.x + (barMax.x - barMin.x) * ratio, barMax.y}, accentU32, 3.f * scale);
}

} 
