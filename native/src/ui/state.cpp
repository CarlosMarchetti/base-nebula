#include "nebula/ui/state.hpp"
#include <windows.h>
#include <fstream>
#include <algorithm>
#include <cmath>
namespace nebula::gui {
WorkspaceState& state(){static WorkspaceState value;return value;}
float unit(float value){return value*state().scale;}
static std::string preferencesPath(){
    HMODULE module=nullptr;char path[MAX_PATH]={};
    GetModuleHandleExA(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS|GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT,
        reinterpret_cast<LPCSTR>(&state),&module);
    if(!GetModuleFileNameA(module,path,MAX_PATH))return {};
    std::string base(path);auto slash=base.find_last_of("\\/");
    return slash==std::string::npos?std::string():base.substr(0,slash+1)+"nebula-ui.ini";
}
void loadPreferences(){auto& s=state();if(s.loaded)return;s.loaded=true;
    std::ifstream in(preferencesPath());std::string line;
    while(std::getline(in,line)){if(line.rfind("scale=",0)==0){try{float n=std::stof(line.substr(6));if(std::isfinite(n))s.scale=std::clamp(n,.8f,1.5f);}catch(...){}}
        else if(line.rfind("favorite=",0)==0)s.favorites.insert(line.substr(9));}
}
void savePreferences(){auto path=preferencesPath();if(path.empty())return;std::ofstream out(path,std::ios::trunc);if(!out)return;
    out<<"scale="<<state().scale<<"\n";for(const auto& name:state().favorites)out<<"favorite="<<name<<"\n";
}
}
