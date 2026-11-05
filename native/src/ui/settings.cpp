#include "nebula/ui/settings.hpp"
#include "nebula/ui/components.hpp"
#include "nebula/ui/state.hpp"
#include "nebula/ui/theme.hpp"
#include "nebula/ui/ui_probe.hpp"
#include <algorithm>
#include <array>
#include <cstdio>
#include <cstdlib>
#include <sstream>
#include <unordered_map>
namespace nebula::gui {
int lwjglKey(int key){
    static const int letters[]={30,48,46,32,18,33,34,35,23,36,37,38,50,49,24,25,16,19,31,20,22,47,17,45,21,44};
    if(key>=ImGuiKey_A&&key<=ImGuiKey_Z)return letters[key-ImGuiKey_A];
    if(key>=ImGuiKey_1&&key<=ImGuiKey_9)return 2+key-ImGuiKey_1;
    if(key==ImGuiKey_0)return 11;
    if(key>=ImGuiKey_F1&&key<=ImGuiKey_F10)return 59+key-ImGuiKey_F1;
    switch(key){case ImGuiKey_F11:return 87;case ImGuiKey_F12:return 88;
    case ImGuiKey_Space:return 57;case ImGuiKey_Tab:return 15;case ImGuiKey_Enter:return 28;case ImGuiKey_Backspace:return 14;
    case ImGuiKey_LeftShift:return 42;case ImGuiKey_RightShift:return 54;case ImGuiKey_LeftCtrl:return 29;case ImGuiKey_RightCtrl:return 157;
    case ImGuiKey_LeftAlt:return 56;case ImGuiKey_RightAlt:return 184;case ImGuiKey_UpArrow:return 200;case ImGuiKey_DownArrow:return 208;
    case ImGuiKey_LeftArrow:return 203;case ImGuiKey_RightArrow:return 205;case ImGuiKey_Insert:return 210;case ImGuiKey_Delete:return 211;
    case ImGuiKey_Home:return 199;case ImGuiKey_End:return 207;case ImGuiKey_PageUp:return 201;case ImGuiKey_PageDown:return 209;
    case ImGuiKey_Minus:return 12;case ImGuiKey_Equal:return 13;case ImGuiKey_LeftBracket:return 26;case ImGuiKey_RightBracket:return 27;
    case ImGuiKey_Semicolon:return 39;case ImGuiKey_Apostrophe:return 40;case ImGuiKey_GraveAccent:return 41;case ImGuiKey_Backslash:return 43;
    case ImGuiKey_Comma:return 51;case ImGuiKey_Period:return 52;case ImGuiKey_Slash:return 53;case ImGuiKey_CapsLock:return 58;default:return -1;}
}
static std::string stringArray(const std::vector<std::string>& values){std::string out="[";bool first=true;
    for(const auto& value:values){if(!first)out+=',';first=false;out+='"';for(unsigned char c:value){if(c=='"'||c=='\\')out+='\\';if(c=='\n')out+="\\n";else if(c=='\r')out+="\\r";else if(c=='\t')out+="\\t";else if(c>=32)out+=static_cast<char>(c);}out+='"';}return out+"]";
}
static void sliderFeedback(){
    static std::unordered_map<ImGuiID,float> activity;
    float& value=activity[ImGui::GetItemID()];value=approach(value,ImGui::IsItemActive()||ImGui::IsItemHovered()?1.f:0.f);
    auto a=ImGui::GetItemRectMin(),b=ImGui::GetItemRectMax();ImVec4 tint=col::ACCENT;tint.w=value*.6f;
    ImGui::GetWindowDrawList()->AddLine({a.x+unit(8),b.y-unit(1)},{b.x-unit(8),b.y-unit(1)},toU32(tint),unit(1));
}
static ImVec4 armorColor(int type){
    switch(type){
    case 0:return {.35f,.83f,.77f,1};
    case 1:return {.43f,.68f,1,1};
    case 2:return {.76f,.58f,1,1};
    default:return {1,.68f,.39f,1};
    }
}
static void armorIcon(ImDrawList* draw,int type,ImVec2 center,float size,ImU32 color){
    const float s=size*.5f,stroke=unit(1.7f);
    auto point=[&](float x,float y){return ImVec2{center.x+x*s,center.y+y*s};};
    auto polygon=[&](const ImVec2* points,int count){
        draw->AddPolyline(points,count,color,ImDrawFlags_Closed,stroke);
    };
    if(type==0){
        const ImVec2 p[]={point(-.7f,.28f),point(-.65f,-.2f),point(-.38f,-.62f),point(0,-.8f),
                          point(.38f,-.62f),point(.65f,-.2f),point(.7f,.28f)};
        polygon(p,7);
        draw->AddLine(point(-.7f,.28f),point(.7f,.28f),color,stroke);
        draw->AddLine(point(-.3f,.02f),point(.3f,.02f),color,stroke);
    }else if(type==1){
        const ImVec2 p[]={point(-.34f,-.62f),point(-.78f,-.42f),point(-.68f,.02f),point(-.44f,-.08f),
                          point(-.38f,.62f),point(.38f,.62f),point(.44f,-.08f),point(.68f,.02f),
                          point(.78f,-.42f),point(.34f,-.62f),point(0,-.35f)};
        polygon(p,11);
        draw->AddLine(point(-.42f,.06f),point(.42f,.06f),color,stroke);
        draw->AddLine(point(0,-.34f),point(0,.58f),color,stroke);
    }else if(type==2){
        const ImVec2 waist[]={point(-.55f,-.65f),point(.55f,-.65f),point(.48f,-.22f),point(.28f,-.08f),
                              point(-.28f,-.08f),point(-.48f,-.22f)};
        polygon(waist,6);
        const ImVec2 left[]={point(-.46f,-.12f),point(-.04f,-.12f),point(-.1f,.72f),point(-.58f,.72f)};
        const ImVec2 right[]={point(.04f,-.12f),point(.46f,-.12f),point(.58f,.72f),point(.1f,.72f)};
        polygon(left,4);polygon(right,4);
        draw->AddLine(point(0,-.08f),point(0,.65f),color,stroke);
    }else{
        const ImVec2 left[]={point(-.55f,-.62f),point(-.08f,-.62f),point(-.08f,.22f),
                             point(-.72f,.22f),point(-.72f,-.16f),point(-.55f,-.16f)};
        const ImVec2 right[]={point(.08f,-.62f),point(.55f,-.62f),point(.55f,-.16f),
                              point(.72f,-.16f),point(.72f,.22f),point(.08f,.22f)};
        polygon(left,6);polygon(right,6);
        draw->AddLine(point(-.55f,-.12f),point(-.1f,-.12f),color,stroke);
        draw->AddLine(point(.1f,-.12f),point(.55f,-.12f),color,stroke);
    }
}
static const char* settingLabel(const std::string& name){
    if(name=="Target Mode")return "Prioridade do alvo";
    return name.c_str();
}
static void control(bridge::ModuleItem& m,bridge::SettingItem& st){
    ImGui::PushID(st.name.c_str());float startX=ImGui::GetCursorPosX(),startY=ImGui::GetCursorPosY(),available=ImGui::GetContentRegionAvail().x;
    bool inlineNumber=st.type=="integer" && available>=unit(225);
    ImGui::PushTextWrapPos(startX+std::max(unit(30),available-(st.type=="boolean"?unit(49):inlineNumber?unit(138):0)));ImGui::TextColored(col::TEXT_DIM,"%s",settingLabel(st.name));ImGui::PopTextWrapPos();ImGui::SetNextItemWidth(-1);
    auto send=[&](const std::string& value){st.value=value;bridge::setSettingValueFromGui(m.name,st.name,value);};
    if(st.type=="boolean"){ImGui::SetCursorPos({startX+available-unit(38),startY});if(toggle("##bool",st.boolVal)){st.boolVal=!st.boolVal;send(st.boolVal?"true":"false");}}
    else if(st.type=="integer"){
        int n=static_cast<int>(st.doubleVal);
        if(inlineNumber){ImGui::SetCursorPos({startX+available-unit(126),startY});ImGui::SetNextItemWidth(unit(126));}
        if(ImGui::DragInt("##int",&n,1,static_cast<int>(st.minVal),static_cast<int>(st.maxVal),st.name=="Durability"?"%d pontos":st.name=="Delay"?"%d ms":"%d",ImGuiSliderFlags_AlwaysClamp)){
            st.doubleVal=n;send(std::to_string(n));
        }
    }
    else if(st.type=="double"){float n=static_cast<float>(st.doubleVal);if(ImGui::SliderFloat("##number",&n,static_cast<float>(st.minVal),static_cast<float>(st.maxVal),"%.2f")){st.doubleVal=n;send(std::to_string(n));}}
    else if(st.type=="range"){float a=0,b=0;std::sscanf(st.value.c_str(),"[ %f , %f ]",&a,&b);ImGui::TextDisabled("Minimo / maximo");ImGui::SetNextItemWidth(-1);
        if(ImGui::DragFloatRange2("##range",&a,&b,.1f,static_cast<float>(st.minVal),static_cast<float>(st.maxVal),"%.2f","%.2f")){send("["+std::to_string(a)+","+std::to_string(b)+"]");}}
    else if(st.type=="combo"){if(ImGui::BeginCombo("##choice",st.value.c_str())){for(const auto& choice:st.options){bool selected=choice==st.value;if(ImGui::Selectable(choice.c_str(),selected))send(choice);if(selected)ImGui::SetItemDefaultFocus();}ImGui::EndCombo();}}
    else if(st.type=="color"){uint32_t n=static_cast<uint32_t>(std::strtoll(st.value.c_str(),nullptr,10));float c[]={((n>>16)&255)/255.f,((n>>8)&255)/255.f,(n&255)/255.f,((n>>24)&255)/255.f};
        if(ImGui::ColorEdit4("##color",c,ImGuiColorEditFlags_NoInputs|ImGuiColorEditFlags_AlphaBar)){uint32_t color=(static_cast<uint32_t>(c[3]*255)<<24)|(static_cast<uint32_t>(c[0]*255)<<16)|(static_cast<uint32_t>(c[1]*255)<<8)|static_cast<uint32_t>(c[2]*255);send(std::to_string(static_cast<int32_t>(color)));}}
    else if(st.type=="keybind"){
        auto& s=state();bool listening=s.listening==m.name;std::string label=listening?"Pressione uma tecla...":"Tecla "+std::to_string(st.keyVal);
        if(ImGui::Button(label.c_str(),{-1,unit(32)}))s.listening=listening?"":m.name;
        if(listening){ImGui::TextDisabled("Esc cancela. Delete remove.");if(ImGui::IsKeyPressed(ImGuiKey_Escape,false))s.listening.clear();
            else for(int k=ImGuiKey_NamedKey_BEGIN;k<ImGuiKey_NamedKey_END;k++){int mapped=k==ImGuiKey_Delete?0:lwjglKey(k);if(mapped>=0&&ImGui::IsKeyPressed(static_cast<ImGuiKey>(k),false)){st.keyVal=m.key=mapped;bridge::setKeybindFromGui(m.name,mapped);s.listening.clear();break;}}}
    }else if(st.type=="list"){
        struct Editor{char input[192]={};};static std::unordered_map<ImGuiID,Editor> editors;auto& e=editors[ImGui::GetID("editor")];
        for(size_t i=0;i<st.options.size();i++){ImGui::PushID(static_cast<int>(i));ImGui::TextWrapped("%s",st.options[i].c_str());ImGui::SameLine();if(ImGui::SmallButton("Remover")){st.options.erase(st.options.begin()+i);send(stringArray(st.options));ImGui::PopID();break;}ImGui::PopID();}
        ImGui::SetNextItemWidth(-1);bool enter=ImGui::InputTextWithHint("##entry","Adicionar item",e.input,sizeof(e.input),ImGuiInputTextFlags_EnterReturnsTrue);
        if((ImGui::Button("Adicionar")||enter)&&e.input[0]){st.options.emplace_back(e.input);e.input[0]=0;send(stringArray(st.options));}
    }else ImGui::TextWrapped("%s",st.value.c_str());
    NEBULA_UI_PROBE("setting:"+st.name);
    if(st.type=="integer"||st.type=="double"||st.type=="range")sliderFeedback();
    ImGui::Spacing();ImGui::PopID();
}
static bool advancedSetting(const bridge::SettingItem& setting){return setting.type=="keybind"||setting.type=="list"||setting.type=="color";}
void settingsPanel(bridge::ModuleItem& m){
    ImGui::PushID(m.name.c_str());
    std::vector<bridge::SettingItem*> controls;
    for(auto& setting:m.settings)
        if(!advancedSetting(setting)) controls.push_back(&setting);
    if(controls.empty()) ImGui::TextDisabled("Sem parametros adicionais.");
    else {
        ImGui::TextColored(col::TEXT_MUTED,"PARAMETROS"); ImGui::Spacing();
        int columns=ImGui::GetContentRegionAvail().x>=unit(470)?2:1;
        ImGui::PushStyleVar(ImGuiStyleVar_CellPadding,{unit(10),unit(8)});
        if(ImGui::BeginTable("parameters",columns,ImGuiTableFlags_SizingStretchSame|ImGuiTableFlags_NoSavedSettings)){
            for(auto* setting:controls){ImGui::TableNextColumn();control(m,*setting);ImGui::Dummy({1,unit(5)});}
            ImGui::EndTable();
        }
        ImGui::PopStyleVar();
    }
    ImGui::Spacing(); ImGui::Separator(); ImGui::Spacing();
    bool advanced=ImGui::CollapsingHeader("Atalhos e ajustes avancados"); NEBULA_UI_PROBE("advanced");
    if(advanced) for(auto& setting:m.settings) if(advancedSetting(setting)) control(m,setting);
    ImGui::Spacing();
    if(ImGui::Button("Restaurar valores padrao",{0,unit(30)})) ImGui::OpenPopup("Confirmar restauracao");
    NEBULA_UI_PROBE("reset");
    if(ImGui::BeginPopup("Confirmar restauracao")){
        ImGui::TextWrapped("Restaurar os ajustes de %s?",m.name.c_str());
        if(ImGui::Button("Restaurar")){bridge::resetSettingsFromGui(m.name);ImGui::CloseCurrentPopup();}
        ImGui::SameLine(); if(ImGui::Button("Cancelar")) ImGui::CloseCurrentPopup();
        ImGui::EndPopup();
    }
    ImGui::PopID();
}
}
