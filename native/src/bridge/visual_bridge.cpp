#include "nebula/ui/visual_frame.hpp"
#include <algorithm>
#include <cmath>
#include <jni.h>
#include <windows.h>

#include <GL/gl.h>
#include <mutex>
#include <unordered_map>
namespace nebula::gui {
bool captureWorldMatrices(std::array<float,36>& values){if(!wglGetCurrentContext())return false;glGetFloatv(GL_MODELVIEW_MATRIX,values.data());glGetFloatv(GL_PROJECTION_MATRIX,values.data()+16);GLint viewport[4];glGetIntegerv(GL_VIEWPORT,viewport);for(int i=0;i<4;i++)values[32+i]=static_cast<float>(viewport[i]);return viewport[2]>0&&viewport[3]>0;}
static std::mutex frameLock;
static std::shared_ptr<const VisualFrame> current = std::make_shared<const VisualFrame>();
std::shared_ptr<const VisualFrame> sharedVisualFrame() {
    std::lock_guard<std::mutex> lock(frameLock);
    return current;
}
VisualFrame visualFrame() {
    return *sharedVisualFrame();
}
void publishVisualFrame(VisualFrame frame, bool projected) {
    std::lock_guard<std::mutex> lock(frameLock);
    auto now = std::chrono::steady_clock::now();
    frame.received = now;
    if (projected)
        frame.projected = now;
    else {
        std::unordered_map<std::string, const VisualPlayer *> old;
        for (const auto &p : current->players)
            old[p.text[0]] = &p;
        for (auto &p : frame.players) {
            auto found = old.find(p.text[0]);
            if (found != old.end()) {
                for (size_t i : {size_t(3), size_t(4), size_t(5)})
                    p.data[i] = found->second->data[i];
                for (size_t i = 11; i < 34; i++)
                    if ((i - 10) % 6 != 0)
                        p.data[i] = found->second->data[i];
                for (size_t i = 35; i < 40; i++)
                    p.data[i] = found->second->data[i];
            }
        }
        frame.items = current->items;
        frame.projected = current->projected;
    }
    current = std::make_shared<const VisualFrame>(std::move(frame));
}
} 
static std::string stringAt(JNIEnv *env, jobjectArray values, jsize index) {
    auto value = static_cast<jstring>(env->GetObjectArrayElement(values, index));
    if (!value)
        return {};
    const char *text = env->GetStringUTFChars(value, nullptr);
    std::string result = text ? text : "";
    if (text)
        env->ReleaseStringUTFChars(value, text);
    env->DeleteLocalRef(value);
    return result;
}
extern "C" JNIEXPORT void JNICALL Java_br_lock_bridge_NativeBridge_publishVisualsNative(
    JNIEnv *env, jclass, jobjectArray names, jfloatArray data, jobjectArray items, jfloatArray itemData,
    jboolean projected) {
    if (!names || !data || !items || !itemData)
        return;
    jsize playerFloats = env->GetArrayLength(data), itemFloats = env->GetArrayLength(itemData);
    jsize n = env->GetArrayLength(names) / 6, m = env->GetArrayLength(items);
    if (n > 256 || m > 512 || env->GetArrayLength(names) % 6 ||
        playerFloats != n * nebula::gui::playerStride || itemFloats != m * nebula::gui::itemStride)
        return;
    std::vector<float> p(playerFloats), d(itemFloats);
    if (playerFloats)
        env->GetFloatArrayRegion(data, 0, playerFloats, p.data());
    if (itemFloats)
        env->GetFloatArrayRegion(itemData, 0, itemFloats, d.data());
    if (env->ExceptionCheck())
        return;
    for (float value : p)
        if (!std::isfinite(value))
            return;
    for (float value : d)
        if (!std::isfinite(value))
            return;
    nebula::gui::VisualFrame frame;
    frame.players.resize(n);
    frame.items.resize(m);
    for (jsize i = 0; i < n; i++) {
        auto &row = frame.players[i];
        for (int j = 0; j < 6; j++)
            row.text[j] = stringAt(env, names, i * 6 + j);
        std::copy_n(p.begin() + i * nebula::gui::playerStride, nebula::gui::playerStride, row.data.begin());
    }
    for (jsize i = 0; i < m; i++) {
        frame.items[i].name = stringAt(env, items, i);
        std::copy_n(d.begin() + i * nebula::gui::itemStride, nebula::gui::itemStride,
                    frame.items[i].data.begin());
    }
    if (!env->ExceptionCheck())
        nebula::gui::publishVisualFrame(std::move(frame), projected == JNI_TRUE);
}

extern "C" JNIEXPORT jfloatArray JNICALL Java_br_lock_bridge_NativeBridge_captureWorldMatricesNative(JNIEnv* env,jclass){std::array<float,36> values{};if(!nebula::gui::captureWorldMatrices(values))return nullptr;jfloatArray result=env->NewFloatArray(36);if(result)env->SetFloatArrayRegion(result,0,36,values.data());return result;}

