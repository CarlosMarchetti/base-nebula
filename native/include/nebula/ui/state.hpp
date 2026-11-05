#pragma once
#include "imgui.h"
#include <string>
#include <unordered_set>
namespace nebula::gui {
struct WorkspaceState {
    std::string category="Combate", selected="", listening;
    char search[128]={};
    std::unordered_set<std::string> favorites;
    float scale=1, inspector=0, categoryFade=1;
    ImFont* body=nullptr;ImFont* display=nullptr;
    bool loaded=false;
};
WorkspaceState& state();
float unit(float value);
void loadPreferences();
void savePreferences();
}
