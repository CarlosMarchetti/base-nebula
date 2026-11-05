#pragma once
#include "nebula/bridge/native_bridge.hpp"
#include "imgui.h"
namespace nebula::gui {
enum class Icon {Grid,Star,Game,Monitor,Sliders,Close,Search,Orbit,Arrow,Shield,Bolt,Swords,Runner,Eye,Chest};
void icon(ImDrawList* draw,Icon type,ImVec2 center,float size,ImU32 color);
bool iconButton(const char* id,Icon type,const char* tooltip,float size=28);
bool toggle(const char* id,bool value);
struct CardAction {bool select=false,toggle=false,favorite=false;};
CardAction moduleCard(const bridge::ModuleItem& module,float width);
Icon moduleIcon(const bridge::ModuleItem& module);
}
