#pragma once
#include "imgui.h"
#include <algorithm>
namespace nebula::gui {
namespace col {
inline const ImVec4 BG={.052f,.057f,.055f,1};
inline const ImVec4 HEADER=BG, CARD={.075f,.080f,.076f,1};
inline const ImVec4 CARD_HOVER={.12f,.12f,.105f,1}, FRAME={.10f,.105f,.098f,1};
inline const ImVec4 CARD_ACTIVE={.17f,.135f,.08f,1}, BORDER={.24f,.245f,.22f,1};
inline const ImVec4 ACCENT={.86f,.66f,.36f,1}, ACCENT_DIM={.48f,.35f,.17f,1};
inline const ImVec4 ACCENT_HOT={.98f,.79f,.49f,1}, TAB_ACTIVE=CARD_ACTIVE;
inline const ImVec4 TEXT={.94f,.93f,.89f,1}, TEXT_DIM={.60f,.62f,.58f,1};
inline const ImVec4 TEXT_MUTED={.40f,.43f,.39f,1}, GREEN={.40f,.76f,.64f,1}, CYAN=GREEN;
}
namespace space {inline constexpr float gap=12, pad=20, radius=12, cardHeight=144, transition=.18f;}
inline ImU32 toU32(ImVec4 v){return ImGui::ColorConvertFloat4ToU32(v);}
inline ImVec4 lerpC(ImVec4 a,ImVec4 b,float t){t=std::clamp(t,0.f,1.f);return {a.x+(b.x-a.x)*t,a.y+(b.y-a.y)*t,a.z+(b.z-a.z)*t,a.w+(b.w-a.w)*t};}
inline float approach(float a,float b){float d=std::min(ImGui::GetIO().DeltaTime,.05f)/space::transition;return a<b?std::min(a+d,b):std::max(a-d,b);}
}
