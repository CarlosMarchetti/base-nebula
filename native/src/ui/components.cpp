#include "nebula/ui/components.hpp"
#include "nebula/ui/theme.hpp"
#include "nebula/ui/state.hpp"
#include <unordered_map>
#include <cmath>
namespace nebula::gui {
void icon(ImDrawList* d,Icon type,ImVec2 c,float z,ImU32 color){
    float r=z*.38f,t=std::max(1.2f,z*.075f);
    auto line=[&](float x,float y,float X,float Y){d->AddLine({c.x+x*r,c.y+y*r},{c.x+X*r,c.y+Y*r},color,t);};
    if(type==Icon::Swords){line(-.9f,-.9f,.7f,.7f);line(.9f,-.9f,-.7f,.7f);line(-.9f,-.4f,-.4f,-.9f);line(.4f,-.9f,.9f,-.4f);line(-.7f,.3f,-.3f,.7f);line(.3f,.7f,.7f,.3f);}
    else if(type==Icon::Runner){d->AddCircleFilled({c.x+r*.35f,c.y-r*.8f},r*.20f,color);line(.15f,-.4f,-.25f,.2f);line(-.25f,.2f,-.7f,.8f);line(-.25f,.2f,.35f,.45f);line(.35f,.45f,.6f,1);line(.05f,-.2f,.55f,.05f);line(.05f,-.2f,-.45f,-.35f);line(-.45f,-.35f,-.75f,0);}
    else if(type==Icon::Eye){ImVec2 p[24];for(int i=0;i<24;i++){float a=6.2831853f*i/24;p[i]={c.x+std::cos(a)*r,c.y+std::sin(a)*r*.6f};}d->AddPolyline(p,24,color,ImDrawFlags_Closed,t);d->AddCircle(c,r*.28f,color,16,t);}
    else if(type==Icon::Chest){d->AddRect({c.x-r*.9f,c.y-r*.7f},{c.x+r*.9f,c.y+r*.75f},color,2,0,t);line(-.9f,-.12f,.9f,-.12f);d->AddRectFilled({c.x-r*.13f,c.y-r*.25f},{c.x+r*.13f,c.y+r*.15f},color,1);}
    else if(type==Icon::Close){line(-.7f,-.7f,.7f,.7f);line(-.7f,.7f,.7f,-.7f);}
    else if(type==Icon::Grid){for(int y=-1;y<2;y+=2)for(int x=-1;x<2;x+=2)d->AddRect({c.x+x*r*.58f-r*.3f,c.y+y*r*.58f-r*.3f},{c.x+x*r*.58f+r*.3f,c.y+y*r*.58f+r*.3f},color,1,0,t);}
    else if(type==Icon::Star){ImVec2 p[10];for(int i=0;i<10;i++){float a=i*3.14159265f/5-1.5707963f,rr=i%2?r*.45f:r;p[i]={c.x+std::cos(a)*rr,c.y+std::sin(a)*rr};}d->AddPolyline(p,10,color,ImDrawFlags_Closed,t);}
    else if(type==Icon::Monitor){d->AddRect({c.x-r,c.y-r*.75f},{c.x+r,c.y+r*.45f},color,2,0,t);line(0,.45f,0,1);line(-.5f,1,.5f,1);}
    else if(type==Icon::Search){d->AddCircle({c.x-r*.2f,c.y-r*.2f},r*.65f,color,16,t);line(.3f,.3f,1,1);}
    else if(type==Icon::Sliders){for(int i=-1;i<2;i++){float y=i*.7f;line(-1,y,1,y);d->AddCircleFilled({c.x+(i%2?.4f:-.4f)*r,c.y+y*r},t*1.5f,color);}}
    else if(type==Icon::Arrow){line(-.35f,-.7f,.35f,0);line(.35f,0,-.35f,.7f);}
    else if(type==Icon::Bolt){line(.2f,-1,-.6f,.1f);line(-.6f,.1f,.3f,.1f);line(.3f,.1f,-.1f,1);}
    else if(type==Icon::Shield){ImVec2 p[]={{c.x,c.y-r},{c.x+r*.8f,c.y-r*.6f},{c.x+r*.65f,c.y+r*.4f},{c.x,c.y+r},{c.x-r*.65f,c.y+r*.4f},{c.x-r*.8f,c.y-r*.6f}};d->AddPolyline(p,6,color,ImDrawFlags_Closed,t);}
    else if(type==Icon::Game){d->AddRect({c.x-r,c.y-r*.6f},{c.x+r,c.y+r*.6f},color,r*.4f,0,t);line(-.6f,0,-.15f,0);line(-.38f,-.25f,-.38f,.25f);d->AddCircleFilled({c.x+r*.5f,c.y-r*.12f},t,color);}
    else {d->AddCircle(c,r*.65f,color,24,t);d->AddEllipse(c,{r*1.15f,r*.45f},color,-.6f,24,t);}
}
bool iconButton(const char* id,Icon type,const char* tip,float size){float z=unit(size);auto p=ImGui::GetCursorScreenPos();bool clicked=ImGui::InvisibleButton(id,{z,z});
    if(ImGui::IsItemHovered())ImGui::GetWindowDrawList()->AddRectFilled(p,{p.x+z,p.y+z},toU32(col::CARD_HOVER),unit(8));
    icon(ImGui::GetWindowDrawList(),type,{p.x+z*.5f,p.y+z*.5f},unit(18),toU32(ImGui::IsItemHovered()?col::TEXT:col::TEXT_DIM));
    if(ImGui::IsItemHovered()&&tip)ImGui::SetTooltip("%s",tip);return clicked;
}
bool toggle(const char* id,bool value){static std::unordered_map<ImGuiID,float> progress;auto key=ImGui::GetID(id);auto inserted=progress.emplace(key,value?1.f:0.f);float& a=inserted.first->second;a=approach(a,value?1.f:0.f);
    auto p=ImGui::GetCursorScreenPos();float w=unit(38),h=unit(21);bool click=ImGui::InvisibleButton(id,{w,h});auto d=ImGui::GetWindowDrawList();
    d->AddRectFilled(p,{p.x+w,p.y+h},toU32(lerpC(col::FRAME,col::ACCENT,a)),h*.5f);d->AddCircleFilled({p.x+h*.5f+(w-h)*a,p.y+h*.5f},h*.5f-unit(3),toU32(col::TEXT));return click;
}
Icon moduleIcon(const bridge::ModuleItem& m){if(m.name=="ArrayList")return Icon::Grid;if(m.name=="Notifications")return Icon::Monitor;if(m.category=="COMBAT")return Icon::Shield;if(m.category=="MOVEMENT")return Icon::Bolt;return Icon::Sliders;}
CardAction moduleCard(const bridge::ModuleItem& m,float w){
    static std::unordered_map<ImGuiID,float> hover,active;
    auto& s=state();auto p=ImGui::GetCursorScreenPos();float h=unit(space::cardHeight);auto id=ImGui::GetID("card");
    ImVec2 togglePos{p.x+w-unit(54),p.y+unit(19)},favPos{p.x+w-unit(42),p.y+unit(99)};
    ImGui::Dummy({w,h});bool hovered=ImGui::IsMouseHoveringRect(p,{p.x+w,p.y+h})&&ImGui::IsWindowHovered(ImGuiHoveredFlags_AllowWhenBlockedByActiveItem);
    bool body=hovered&&ImGui::IsMouseClicked(ImGuiMouseButton_Left);
    float& a=active.emplace(id,m.enabled?1.f:0.f).first->second;a=approach(a,m.enabled?1.f:0.f);float& v=hover[id];v=approach(v,hovered?1.f:0.f);
    auto d=ImGui::GetWindowDrawList();bool selected=s.selected==m.name;
    d->AddRectFilled(p,{p.x+w,p.y+h},toU32(lerpC(lerpC(col::CARD,col::CARD_ACTIVE,a),col::CARD_HOVER,v*.45f)),unit(space::radius));
    d->AddRect(p,{p.x+w,p.y+h},toU32(selected?col::ACCENT:lerpC(col::BORDER,col::ACCENT_DIM,a)),unit(space::radius),0,unit(selected?1.5f:1.f));
    icon(d,moduleIcon(m),{p.x+unit(27),p.y+unit(30)},unit(23),toU32(m.enabled?col::ACCENT:col::TEXT_DIM));
    const ImVec4 clip{p.x+unit(48),p.y,p.x+w-unit(62),p.y+unit(58)};
    d->AddText(s.body?s.body:ImGui::GetFont(),unit(17),{p.x+unit(48),p.y+unit(20)},toU32(col::TEXT),m.name.c_str(),nullptr,0,&clip);
    d->PushClipRect({p.x+unit(16),p.y+unit(59)},{p.x+w-unit(16),p.y+unit(99)},true);
    d->AddText(s.body?s.body:ImGui::GetFont(),unit(14),{p.x+unit(16),p.y+unit(62)},toU32(col::TEXT_DIM),m.description.c_str(),nullptr,w-unit(32));d->PopClipRect();
    d->AddCircleFilled({p.x+unit(19),p.y+unit(120)},unit(2.5f),toU32(m.enabled?col::ACCENT:col::TEXT_MUTED));
    d->AddText(s.body?s.body:ImGui::GetFont(),unit(12),{p.x+unit(27),p.y+unit(112)},toU32(m.enabled?col::ACCENT:col::TEXT_MUTED),m.enabled?"ATIVO":"DESATIVADO");
    ImGui::SetCursorScreenPos(togglePos);bool toggled=toggle("toggle",m.enabled);
    ImGui::SetCursorScreenPos(favPos);bool favorite=iconButton("favorite",Icon::Star,s.favorites.count(m.name)?"Remover dos favoritos":"Adicionar aos favoritos",26);
    if(s.favorites.count(m.name))icon(d,Icon::Star,{favPos.x+unit(13),favPos.y+unit(13)},unit(18),toU32(col::ACCENT));
    bool inToggle=ImGui::IsMouseHoveringRect(togglePos,{togglePos.x+unit(38),togglePos.y+unit(21)});
    bool inFavorite=ImGui::IsMouseHoveringRect(favPos,{favPos.x+unit(26),favPos.y+unit(26)});
    ImGui::SetCursorScreenPos({p.x,p.y+h+unit(space::gap)});return {body&&!inToggle&&!inFavorite,toggled,favorite};
}
}
