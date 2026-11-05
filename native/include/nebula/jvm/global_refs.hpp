#pragma once
#include <jni.h>
#include <map>
#include <string>
namespace nebula::jvm {
class GlobalClassRefs {
    JNIEnv* env;
    std::map<std::string,jclass>& classes;
public:
    GlobalClassRefs(JNIEnv* e,std::map<std::string,jclass>& c):env(e),classes(c){}
    GlobalClassRefs(const GlobalClassRefs&)=delete;
    GlobalClassRefs& operator=(const GlobalClassRefs&)=delete;
    ~GlobalClassRefs(){for(auto& entry:classes)if(entry.second)env->DeleteGlobalRef(entry.second);classes.clear();}
};
}
