#include "nebula/ui/nebula_gui.hpp"
#include "nebula/bridge/native_bridge.hpp"
#include "nebula/ui/components.hpp"
#include "nebula/ui/settings.hpp"
#include "nebula/ui/state.hpp"
#include "nebula/ui/theme.hpp"
#include <algorithm>
#include <cctype>
#include <vector>
#include <cmath>
#include <cstdio>
#include "nebula/ui/ui_probe.hpp"
namespace nebula::gui {
using bridge::ModuleItem;
void setFonts(ImFont *body, ImFont *display) {
    state().body = body;
    state().display = display;
}
void initTheme() {
    ImGuiStyle &s = ImGui::GetStyle();
    s.WindowRounding = 16;
    s.ChildRounding = 12;
    s.FrameRounding = 8;
    s.PopupRounding = 10;
    s.GrabRounding = 6;
    s.ScrollbarRounding = 8;
    s.WindowPadding = {0, 0};
    s.FramePadding = {10, 7};
    s.ItemSpacing = {10, 8};
    s.ItemInnerSpacing = {8, 6};
    s.ScrollbarSize = 8;
    s.WindowBorderSize = 1;
    s.ChildBorderSize = 1;
    s.FrameBorderSize = 0;
    auto c = s.Colors;
    c[ImGuiCol_Text] = col::TEXT;
    c[ImGuiCol_TextDisabled] = col::TEXT_DIM;
    c[ImGuiCol_WindowBg] = col::BG;
    c[ImGuiCol_ChildBg] = col::CARD;
    c[ImGuiCol_PopupBg] = col::CARD;
    c[ImGuiCol_Border] = col::BORDER;
    c[ImGuiCol_BorderShadow] = {0, 0, 0, 0};
    c[ImGuiCol_FrameBg] = col::FRAME;
    c[ImGuiCol_FrameBgHovered] = col::CARD_HOVER;
    c[ImGuiCol_FrameBgActive] = col::CARD_ACTIVE;
    c[ImGuiCol_Button] = col::FRAME;
    c[ImGuiCol_ButtonHovered] = col::CARD_HOVER;
    c[ImGuiCol_ButtonActive] = col::ACCENT_DIM;
    c[ImGuiCol_Header] = col::CARD_ACTIVE;
    c[ImGuiCol_HeaderHovered] = col::CARD_HOVER;
    c[ImGuiCol_HeaderActive] = col::ACCENT_DIM;
    c[ImGuiCol_CheckMark] = col::ACCENT;
    c[ImGuiCol_SliderGrab] = col::ACCENT;
    c[ImGuiCol_SliderGrabActive] = col::ACCENT_HOT;
    c[ImGuiCol_Separator] = col::BORDER;
    c[ImGuiCol_ResizeGrip] = {0, 0, 0, 0};
    c[ImGuiCol_ResizeGripHovered] = col::ACCENT_DIM;
    c[ImGuiCol_ResizeGripActive] = col::ACCENT;
    c[ImGuiCol_ScrollbarBg] = {0, 0, 0, 0};
    c[ImGuiCol_ScrollbarGrab] = col::BORDER;
    c[ImGuiCol_ScrollbarGrabHovered] = col::TEXT_DIM;
    c[ImGuiCol_ScrollbarGrabActive] = col::ACCENT;
}
static std::string lower(std::string value) {
    std::transform(value.begin(),value.end(),value.begin(),[](unsigned char c){return static_cast<char>(std::tolower(c));});return value;
}
static bool matchesCategory(const ModuleItem& m){
    const auto& c=state().category;
    if(c=="Favoritos")return state().favorites.count(m.name)>0;
    if(c=="Todos")return true;
    if(c=="Jogo")return m.category=="COMBAT"||m.category=="MOVEMENT";
    if(c=="HUD")return m.category=="RENDER";
    if(c=="Visuals")return m.category=="VISUALS";
    if(c=="Combate")return m.category=="COMBAT";
    if(c=="Movimento")return m.category=="MOVEMENT";
    return c==m.category;
}
static const char* categoryName(const std::string& c){
    if(c=="COMBAT")return "Combate";if(c=="MOVEMENT")return "Movimento";
    if(c=="VISUALS")return "Visuais";if(c=="RENDER")return "HUD";return c.c_str();
}
struct Nav {std::string key,label;Icon glyph;};
static std::vector<Nav> categories(const std::vector<ModuleItem>& modules){
    std::vector<Nav> result={{"Favoritos","Favoritos",Icon::Star},{"Todos","Todos",Icon::Grid}};
    std::vector<std::string> order={"COMBAT","MOVEMENT","VISUALS","RENDER"};
    for(const auto& m:modules)if(std::find(order.begin(),order.end(),m.category)==order.end())order.push_back(m.category);
    for(const auto& category:order){
        if(!std::any_of(modules.begin(),modules.end(),[&](const ModuleItem& m){return m.category==category;}))continue;
        std::string key=category=="COMBAT"?"Combate":category=="MOVEMENT"?"Movimento":category=="VISUALS"?"Visuals":category=="RENDER"?"HUD":category;
        result.push_back({key,categoryName(category),category=="COMBAT"?Icon::Swords:category=="MOVEMENT"?Icon::Runner:category=="VISUALS"?Icon::Eye:Icon::Monitor});
    }return result;
}
static void chooseCategory(const Nav& nav){auto& s=state();if(s.category!=nav.key){s.category=nav.key;s.categoryFade=0;}}
static void navigationArc(const std::vector<Nav>& navs,ImVec2 size){
    auto origin=ImGui::GetCursorScreenPos();auto draw=ImGui::GetWindowDrawList();
    float top=unit(24),span=std::max(unit(80),size.y-unit(86));
    auto point=[&](float t){return ImVec2{origin.x+unit(77)+std::sin(t*3.14159265f)*unit(54),origin.y+top+t*span};};
    ImVec2 path[65];for(int i=0;i<=64;i++)path[i]=point(i/64.f);
    draw->AddPolyline(path,65,toU32(col::BORDER),ImDrawFlags_None,unit(1.2f));
    for(size_t i=0;i<navs.size();i++){
        const auto& n=navs[i];float t=navs.size()>1?static_cast<float>(i)/(navs.size()-1):.5f;
        auto anchor=point(t);float y=origin.y+top+t*span, x=origin.x+unit(40);bool selected=state().category==n.key;
        ImGui::PushID(n.key.c_str());ImGui::SetCursorScreenPos({origin.x,y-unit(24)});
        bool clicked=ImGui::InvisibleButton("category",{unit(76),unit(64)});NEBULA_UI_PROBE("nav:"+n.key);
        bool hover=ImGui::IsItemHovered();float radius=unit(selected?23:19);
        if(selected||hover)draw->AddCircleFilled({x,y},radius,toU32(selected?col::CARD_ACTIVE:col::CARD_HOVER),32);
        draw->AddCircle({x,y},radius,toU32(selected?col::ACCENT:hover?col::TEXT_DIM:col::BORDER),32,unit(selected?1.7f:1));
        icon(draw,n.glyph,{x,y},unit(selected?23:20),toU32(selected?col::ACCENT:col::TEXT_DIM));
        auto text=ImGui::CalcTextSize(n.label.c_str());draw->AddText({x-text.x*.5f,y+unit(29)},toU32(selected?col::ACCENT:col::TEXT_DIM),n.label.c_str());
        draw->AddCircleFilled(anchor,unit(selected?3.5f:2.2f),toU32(selected?col::ACCENT:col::BORDER));
        if(selected){draw->AddLine({x+radius+unit(8),y},{anchor.x,y},toU32(col::ACCENT_DIM),unit(1));draw->AddLine(anchor,{origin.x+size.x,y},toU32(col::ACCENT_DIM),unit(1));}
        if(clicked)chooseCategory(n);ImGui::PopID();
    }
}
static void compactCategories(const std::vector<Nav>& navs,float width){
    auto& s=state();const char* preview=s.category.c_str();for(const auto& n:navs)if(n.key==s.category)preview=n.label.c_str();
    ImGui::SetNextItemWidth(width);
    if(ImGui::BeginCombo("##category",preview)){for(const auto& n:navs)if(ImGui::Selectable(n.label.c_str(),s.category==n.key))chooseCategory(n);ImGui::EndCombo();}
    NEBULA_UI_PROBE("category_picker");
}
static void moduleList(std::vector<ModuleItem*>& visible,ImVec2 size){
    auto& s=state();ImGui::BeginChild("module_list",size,ImGuiChildFlags_None,ImGuiWindowFlags_NoBackground);
    ImGui::TextColored(col::TEXT_MUTED,"MODULOS / %02d",static_cast<int>(visible.size()));ImGui::Spacing();
    if(visible.empty()){ImGui::TextWrapped(s.category=="Favoritos"?"Marque a estrela de um modulo para salva-lo aqui.":"Nenhum resultado para esta busca.");}
    for(auto* module:visible){
        auto& m=*module;ImGui::PushID(m.name.c_str());auto p=ImGui::GetCursorScreenPos();float width=ImGui::GetContentRegionAvail().x,h=unit(43);
        bool selected=s.selected==m.name;bool click=ImGui::InvisibleButton("row",{width,h});NEBULA_UI_PROBE("module:"+m.name);
        bool hover=ImGui::IsItemHovered();auto draw=ImGui::GetWindowDrawList();
        if(selected||hover)draw->AddRectFilled(p,{p.x+width,p.y+h},toU32(selected?col::CARD_ACTIVE:col::CARD_HOVER),unit(7));
        if(selected)draw->AddRect(p,{p.x+width,p.y+h},toU32(col::ACCENT_DIM),unit(7),0,unit(1));
        draw->AddCircleFilled({p.x+unit(13),p.y+h*.5f},unit(m.enabled?3:2),toU32(m.enabled?col::ACCENT:col::TEXT_MUTED));
        ImVec4 clip{p.x+unit(25),p.y,p.x+width-unit(10),p.y+h};
        draw->AddText(s.body?s.body:ImGui::GetFont(),unit(15),{p.x+unit(25),p.y+unit(13)},toU32(selected?col::TEXT:col::TEXT_DIM),m.name.c_str(),nullptr,0,&clip);
        if(hover)ImGui::SetTooltip("%s\nClique para configurar. Botao direito: acoes.",m.description.c_str());
        if(click){s.selected=m.name;s.listening.clear();}
        if(ImGui::BeginPopupContextItem("actions")){
            if(ImGui::MenuItem(m.enabled?"Desativar":"Ativar")){m.enabled=!m.enabled;bridge::setModuleEnabledFromGui(m.name,m.enabled);}
            bool favorite=s.favorites.count(m.name)>0;
            if(ImGui::MenuItem(favorite?"Remover favorito":"Favoritar")){if(favorite)s.favorites.erase(m.name);else s.favorites.insert(m.name);savePreferences();}
            ImGui::EndPopup();
        }
        ImGui::PopID();
    }ImGui::EndChild();
}
static void workbench(ModuleItem* selected,ImVec2 size){
    ImGui::PushStyleColor(ImGuiCol_ChildBg,col::CARD);ImGui::PushStyleVar(ImGuiStyleVar_WindowPadding,{unit(25),unit(22)});
    ImGui::BeginChild("orbit_workbench",size,ImGuiChildFlags_Border,ImGuiWindowFlags_AlwaysUseWindowPadding);
    static std::string lastModule;static float contentFade=1;
    std::string nextModule=selected?selected->name:"";
    if(lastModule!=nextModule){lastModule=nextModule;contentFade=0;ImGui::SetScrollY(0);}
    contentFade=approach(contentFade,1);ImGui::PushStyleVar(ImGuiStyleVar_Alpha,std::max(.2f,contentFade));
    if(!selected){ImGui::TextUnformatted("Seu espaco de configuracao");ImGui::TextWrapped("Selecione um modulo na lista para ajustar suas opcoes.");}
    else {
        auto& m=*selected;auto& s=state();ImGui::PushID(m.name.c_str());
        float width=ImGui::GetContentRegionAvail().x;
        ImGui::TextColored(col::TEXT_MUTED,"%s / AJUSTES",categoryName(m.category));ImGui::Spacing();
        auto p=ImGui::GetCursorScreenPos();float actionWidth=unit(116),titleSize=unit(width<unit(420)?26:40);
        auto draw=ImGui::GetWindowDrawList();ImVec4 clip{p.x,p.y,p.x+std::max(unit(60),width-actionWidth),p.y+unit(47)};
        draw->AddText(s.display?s.display:ImGui::GetFont(),titleSize,p,toU32(col::TEXT),m.name.c_str(),nullptr,0,&clip);
        ImVec2 pill{p.x+width-unit(128),p.y};
        draw->AddRectFilled(pill,{p.x+width+unit(8),p.y+unit(35)},toU32(m.enabled?col::CARD_ACTIVE:col::FRAME),unit(18));
        draw->AddRect(pill,{p.x+width+unit(8),p.y+unit(35)},toU32(m.enabled?col::ACCENT_DIM:col::BORDER),unit(18));
        ImGui::SetCursorScreenPos({p.x+width-unit(38),p.y+unit(7)});
        if(toggle("module_enabled",m.enabled)){m.enabled=!m.enabled;bridge::setModuleEnabledFromGui(m.name,m.enabled);}NEBULA_UI_PROBE("enabled");
        draw->AddText(s.body?s.body:ImGui::GetFont(),unit(12),{p.x+width-unit(112),p.y+unit(10)},toU32(m.enabled?col::ACCENT:col::TEXT_MUTED),m.enabled?"ATIVADO":"INATIVO");
        ImGui::SetCursorScreenPos({p.x,p.y+unit(49)});ImGui::PushTextWrapPos(ImGui::GetCursorPosX()+width-unit(45));ImGui::TextColored(col::TEXT_DIM,"%s",m.description.c_str());ImGui::PopTextWrapPos();
        auto after=ImGui::GetCursorScreenPos();ImGui::SetCursorScreenPos({p.x+width-unit(26),p.y+unit(43)});
        bool favorite=s.favorites.count(m.name)>0;
        ImGui::PushStyleColor(ImGuiCol_Text,favorite?col::ACCENT:col::TEXT_DIM);
        if(iconButton("favorite",Icon::Star,favorite?"Remover favorito":"Favoritar",24)){if(favorite)s.favorites.erase(m.name);else s.favorites.insert(m.name);savePreferences();}NEBULA_UI_PROBE("favorite");ImGui::PopStyleColor();
        if(favorite)icon(draw,Icon::Star,{p.x+width-unit(14),p.y+unit(55)},unit(18),toU32(col::ACCENT));
        ImGui::SetCursorScreenPos({p.x,after.y+unit(9)});ImGui::Separator();ImGui::Spacing();
        settingsPanel(m);ImGui::PopID();
    }ImGui::PopStyleVar();ImGui::EndChild();ImGui::PopStyleVar();ImGui::PopStyleColor();
}
void renderGui(){
    loadPreferences();auto& s=state();static std::shared_ptr<const std::vector<ModuleItem>> previous;static std::vector<ModuleItem> modules;
    auto shared=bridge::getSharedModulesSnapshot();if(previous!=shared){modules=*shared;previous=shared;}
    auto& io=ImGui::GetIO();float pad=unit(20),gap=unit(18);
    ImVec2 maxSize{std::max(180.f,io.DisplaySize.x-16),std::max(180.f,io.DisplaySize.y-16)};
    ImVec2 initial{std::min(unit(1240),maxSize.x),std::min(unit(840),maxSize.y)};
    ImGui::SetNextWindowSizeConstraints({std::min(unit(420),maxSize.x),std::min(unit(340),maxSize.y)},maxSize);
    ImGui::SetNextWindowSize(initial,ImGuiCond_FirstUseEver);ImGui::SetNextWindowPos({(io.DisplaySize.x-initial.x)*.5f,(io.DisplaySize.y-initial.y)*.5f},ImGuiCond_FirstUseEver);
    ImGui::PushStyleVar(ImGuiStyleVar_WindowPadding,{0,0});ImGui::PushStyleVar(ImGuiStyleVar_WindowRounding,unit(13));
    ImGui::PushStyleVar(ImGuiStyleVar_FramePadding,{unit(9),unit(6)});ImGui::PushStyleVar(ImGuiStyleVar_ItemSpacing,{unit(10),unit(9)});
    bool open=true;bool shown=ImGui::Begin("Nebula Client##orbit",&open,ImGuiWindowFlags_NoTitleBar|ImGuiWindowFlags_NoCollapse|ImGuiWindowFlags_NoScrollbar);
    if(!open)bridge::setGuiOpen(false);
    if(shown){
        ImGui::SetWindowFontScale(s.scale);auto size=ImGui::GetWindowSize(),pos=ImGui::GetWindowPos();
        ImGui::SetWindowPos({std::clamp(pos.x,8.f,std::max(8.f,io.DisplaySize.x-size.x-8)),std::clamp(pos.y,8.f,std::max(8.f,io.DisplaySize.y-size.y-8))});pos=ImGui::GetWindowPos();
        auto draw=ImGui::GetWindowDrawList();float brand=std::min(unit(260),size.x-unit(110));
        ImGui::SetCursorPos({0,0});ImGui::InvisibleButton("drag",{brand,unit(62)});
        if(ImGui::IsItemActive()&&ImGui::IsMouseDragging(ImGuiMouseButton_Left))ImGui::SetWindowPos({pos.x+io.MouseDelta.x,pos.y+io.MouseDelta.y});
        icon(draw,Icon::Orbit,{pos.x+pad+unit(12),pos.y+unit(30)},unit(23),toU32(col::GREEN));
        draw->AddText(s.body?s.body:ImGui::GetFont(),unit(17),{pos.x+pad+unit(35),pos.y+unit(21)},toU32(col::TEXT),"N E B U L A");
        if(size.x>unit(650))draw->AddText(s.body?s.body:ImGui::GetFont(),unit(12),{pos.x+pad+unit(139),pos.y+unit(25)},toU32(col::TEXT_MUTED),"CLIENT / ORBIT");
        if(size.x>unit(700)){
            ImGui::SetCursorPos({unit(315),unit(16)});ImGui::SetNextItemWidth(std::max(unit(100),size.x-unit(445)));
            if(io.KeyCtrl&&ImGui::IsKeyPressed(ImGuiKey_K))ImGui::SetKeyboardFocusHere();
            ImGui::InputTextWithHint("##search","Buscar modulos...",s.search,sizeof(s.search));NEBULA_UI_PROBE("search");
        }
        ImGui::SetCursorPos({size.x-unit(90),unit(17)});
        if(iconButton("preferences",Icon::Sliders,"Interface e configuracao",26))ImGui::OpenPopup("Interface");NEBULA_UI_PROBE("preferences");ImGui::SameLine(0,unit(8));
        if(iconButton("close_menu",Icon::Close,"Fechar menu",26))bridge::setGuiOpen(false);NEBULA_UI_PROBE("close");
        if(ImGui::BeginPopup("Interface")){
            ImGui::TextUnformatted("ORBIT / Interface");float scale=s.scale;ImGui::SetNextItemWidth(190);
            if(ImGui::SliderFloat("Escala",&scale,.8f,1.5f,"%.2fx")){s.scale=scale;savePreferences();}
            if(ImGui::Button("Salvar configuracao"))bridge::saveConfigFromGui();if(ImGui::Button("Recarregar configuracao"))bridge::loadConfigFromGui();ImGui::EndPopup();
        }
        draw->AddLine({pos.x+pad,pos.y+unit(62)},{pos.x+size.x-pad,pos.y+unit(62)},toU32(col::BORDER));
        bool compact=size.x<unit(840);float top=unit(78),height=std::max(unit(80),size.y-top-unit(37));auto navs=categories(modules);
        if(compact){ImGui::SetCursorPos({pad,top});compactCategories(navs,std::min(unit(140),(size.x-pad*2)*.4f));ImGui::SameLine();ImGui::SetNextItemWidth(-pad);ImGui::InputTextWithHint("##compact_search","Buscar...",s.search,sizeof(s.search));NEBULA_UI_PROBE("search");top+=unit(47);height-=unit(47);}
        std::vector<ModuleItem*> visible;std::string search=lower(s.search);ModuleItem* selected=nullptr;
        for(auto& m:modules){if(m.name==s.selected)selected=&m;if(matchesCategory(m)&&(search.empty()||lower(m.name+" "+m.description).find(search)!=std::string::npos))visible.push_back(&m);}
        if(s.selected.empty()&&!visible.empty()){selected=visible.front();s.selected=selected->name;}
        s.categoryFade=approach(s.categoryFade,1);s.inspector=approach(s.inspector,selected?1.f:0.f);
        float sidebar=compact?std::clamp((size.x-pad*2)*.28f,unit(120),unit(178)):unit(349);
        ImGui::SetCursorPos({pad,top});
        if(!compact){
            ImGui::BeginChild("orbit_navigation",{unit(156),height},ImGuiChildFlags_None,ImGuiWindowFlags_NoScrollbar|ImGuiWindowFlags_NoBackground);navigationArc(navs,{unit(156),height});ImGui::EndChild();
            ImGui::SetCursorPos({pad+unit(167),top});moduleList(visible,{sidebar-unit(167),height});
        }else moduleList(visible,{sidebar,height});
        ImGui::SetCursorPos({pad+sidebar+gap,top});workbench(selected,{std::max(unit(120),size.x-pad*2-sidebar-gap),height});
        ImGui::SetCursorPos({pad,size.y-unit(25)});ImGui::TextColored(col::TEXT_MUTED,"NEBULA / ORBIT");
        if(size.x>unit(640)){ImGui::SameLine();ImGui::SetCursorPosX(size.x-unit(250));ImGui::TextDisabled("%d modulos  /  Minecraft 1.8.9",static_cast<int>(modules.size()));}
    }ImGui::End();ImGui::PopStyleVar(4);
}
} 
