#include "nebula/security/crypto.hpp"
#include "nebula/jvm/global_refs.hpp"
#include <fstream>
#include <iterator>
#include <iostream>
#include <stdexcept>
#include <cstring>
static int deleted=0;
static void JNICALL release(JNIEnv*,jobject){++deleted;}
static void check(bool b,const char* msg){if(!b)throw std::runtime_error(msg);}
int main(int argc,char** argv){try{
    check(argc==2,"PayloadCheck payload.bin");
    uint8_t digest[32];nebula::security::sha256(reinterpret_cast<const uint8_t*>("abc"),3,digest);
    const uint8_t expected[]={0xba,0x78,0x16,0xbf,0x8f,0x01,0xcf,0xea,0x41,0x41,0x40,0xde,0x5d,0xae,0x22,0x23,0xb0,0x03,0x61,0xa3,0x96,0x17,0x7a,0x9c,0xb4,0x10,0xff,0x61,0xf2,0x00,0x15,0xad};check(!std::memcmp(digest,expected,32),"SHA256 known vector");
    std::ifstream in(argv[1],std::ios::binary);std::vector<uint8_t> bytes((std::istreambuf_iterator<char>(in)),{}),plain;
    const char keyText[]="NebulaBase-Client-Key-2026-Lunar";uint8_t key[32]={};std::memcpy(key,keyText,32);
    check(nebula::security::decryptPayload(bytes.data(),bytes.size(),key,plain),"Python/C++ payload compatibility");check(plain.size()>4,"Empty payload");
    bytes.back()^=1;check(!nebula::security::decryptPayload(bytes.data(),bytes.size(),key,plain)&&plain.empty(),"Corrupt payload accepted");
    JNINativeInterface_ table={};table.DeleteGlobalRef=release;JNIEnv env={&table};std::map<std::string,jclass> refs{{"a",reinterpret_cast<jclass>(1)},{"b",reinterpret_cast<jclass>(2)}};
    {nebula::jvm::GlobalClassRefs owned(&env,refs);}check(deleted==2&&refs.empty(),"Temporary JNI global reference leak");
    std::cout<<"PASS SHA256 vector, Python/C++ format, corruption rejection, JNI reference cleanup\n";return 0;
}catch(const std::exception& e){std::cerr<<e.what()<<'\n';return 1;}}
