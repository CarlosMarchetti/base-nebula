#pragma once
#ifdef NEBULA_UI_TEST
#include "imgui.h"
#include <string>
#include <unordered_map>
namespace nebula::gui {
struct UiProbeRect { ImVec2 min, max; };
inline std::unordered_map<std::string,UiProbeRect>& uiProbe() { static std::unordered_map<std::string,UiProbeRect> value; return value; }
inline void recordUiProbe(const std::string& key) { if(ImGui::IsItemVisible()) uiProbe()[key]={ImGui::GetItemRectMin(),ImGui::GetItemRectMax()}; }
}
#define NEBULA_UI_PROBE(key) ::nebula::gui::recordUiProbe(key)
#else
#define NEBULA_UI_PROBE(key) ((void)0)
#endif
