#include "nebula/bridge/native_bridge.hpp"
#include "nebula/core/logger.hpp"
#include "nebula/jvm/jvm_manager.hpp"
#include <algorithm>
#include <atomic>
#include <cstdint>
#include <memory>
#include <sstream>

namespace nebula {
namespace bridge {

static CRITICAL_SECTION g_bridgeLock;
static bool g_initialized = false;
static std::atomic_bool g_guiOpen{false};
static bool g_snapshotDirty = true;
static std::shared_ptr<const std::vector<ModuleItem>> g_snapshot;

static std::vector<ModuleItem> g_modules;
static ClanData g_clanData;
static std::vector<NotificationItem> g_notifications;
static DamageSnapshot g_damageSnapshot;

static jclass g_bridgeCls = nullptr;
static jmethodID g_setModuleEnabledMid = nullptr;
static jmethodID g_setSettingValueMid = nullptr;
static jmethodID g_addClanEntryMid = nullptr;
static jmethodID g_removeClanEntryMid = nullptr;
static jmethodID g_saveConfigMid = nullptr;
static jmethodID g_loadConfigMid = nullptr;
static jmethodID g_setKeybindMid = nullptr;

void init() {
    if (g_initialized)
        return;
    InitializeCriticalSection(&g_bridgeLock);
    g_initialized = true;

    logger::info("Bridge C++/Java inicializada.");
}

void shutdown() {
    if (!g_initialized)
        return;
    jvm::ScopedJniEnv env;
    if (env.isValid() && g_bridgeCls) {
        env->DeleteGlobalRef(g_bridgeCls);
        g_bridgeCls = nullptr;
    }
    DeleteCriticalSection(&g_bridgeLock);
    g_initialized = false;
}

bool isGuiOpen() {
    return g_guiOpen;
}

void setGuiOpen(bool open) {
    if (g_guiOpen.exchange(open) == open)
        return;
    logger::info("GUI status alterado para: %s", open ? "ABERTA" : "FECHADA");

    setModuleEnabledFromGui("ClickGui", open);
}

void toggleGui() {
    setGuiOpen(!g_guiOpen);
}

bool isHudEnabled() {
    ScopedLock lock(&g_bridgeLock);
    for (const auto &m : g_modules) {
        if (m.name == "ArrayList")
            return m.enabled;
    }
    return true;
}

std::shared_ptr<const std::vector<ModuleItem>> getSharedModulesSnapshot() {
    ScopedLock lock(&g_bridgeLock);
    if (g_snapshotDirty || !g_snapshot) {
        g_snapshot = std::make_shared<const std::vector<ModuleItem>>(g_modules);
        g_snapshotDirty = false;
    }
    return g_snapshot;
}
std::vector<ModuleItem> getModulesSnapshot() {
    return *getSharedModulesSnapshot();
}

ClanData getClanDataSnapshot() {
    ScopedLock lock(&g_bridgeLock);
    return g_clanData;
}

void pushNotification(const std::string &type, const std::string &message) {
    if (message.empty())
        return;
    ScopedLock lock(&g_bridgeLock);
    auto now = std::chrono::steady_clock::now();
    g_notifications.erase(
        std::remove_if(
            g_notifications.begin(), g_notifications.end(),
            [now](const NotificationItem &item) {
                return std::chrono::duration_cast<std::chrono::milliseconds>(now - item.createdAt).count() >
                       4200;
            }),
        g_notifications.end());
    if (g_notifications.size() >= 5)
        g_notifications.erase(g_notifications.begin());
    g_notifications.push_back({type, message, now});
}

std::vector<NotificationItem> getNotificationsSnapshot() {
    ScopedLock lock(&g_bridgeLock);
    auto now = std::chrono::steady_clock::now();
    g_notifications.erase(
        std::remove_if(
            g_notifications.begin(), g_notifications.end(),
            [now](const NotificationItem &item) {
                return std::chrono::duration_cast<std::chrono::milliseconds>(now - item.createdAt).count() >
                       4200;
            }),
        g_notifications.end());
    return g_notifications;
}

void updateDamageSnapshot(const std::string &name, float health, bool visible) {
    ScopedLock lock(&g_bridgeLock);
    g_damageSnapshot.name = name;
    g_damageSnapshot.health = std::max(0.f, std::min(health, 2048.f));
    g_damageSnapshot.visible = visible && !name.empty();
}

DamageSnapshot getDamageSnapshot() {
    ScopedLock lock(&g_bridgeLock);
    return g_damageSnapshot;
}

extern "C" {
JNIEXPORT void JNICALL Java_br_lock_bridge_NativeBridge_syncModuleState(JNIEnv *, jclass, jstring, jboolean);
JNIEXPORT void JNICALL Java_br_lock_bridge_NativeBridge_syncModuleDefinitionNative(JNIEnv *, jclass, jstring,
                                                                                   jstring, jstring, jboolean,
                                                                                   jint, jobjectArray);
JNIEXPORT jboolean JNICALL Java_br_lock_bridge_NativeBridge_isGuiOpenNative(JNIEnv *, jclass);
JNIEXPORT void JNICALL Java_br_lock_bridge_NativeBridge_setGuiOpenNative(JNIEnv *, jclass, jboolean);
JNIEXPORT void JNICALL Java_br_lock_bridge_NativeBridge_onLogNative(JNIEnv *, jclass, jstring);
JNIEXPORT void JNICALL Java_br_lock_bridge_NativeBridge_sendNotificationNative(JNIEnv *, jclass, jstring,
                                                                               jstring);
JNIEXPORT void JNICALL Java_br_lock_bridge_NativeBridge_updateDamageSnapshotNative(JNIEnv *, jclass, jstring,
                                                                                   jfloat, jboolean);
JNIEXPORT void JNICALL Java_br_lock_bridge_NativeBridge_publishVisualsNative(JNIEnv *, jclass, jobjectArray,
                                                                             jfloatArray, jobjectArray,
                                                                             jfloatArray, jboolean);
}

extern "C" JNIEXPORT jfloatArray JNICALL Java_br_lock_bridge_NativeBridge_captureWorldMatricesNative(JNIEnv*,jclass);

void bindJavaBridge(JNIEnv *env, jclass bridgeCls) {
    if (!env || !bridgeCls)
        return;
    if (g_bridgeCls) {
        env->DeleteGlobalRef(g_bridgeCls);
        g_bridgeCls = nullptr;
    }
    g_bridgeCls = reinterpret_cast<jclass>(env->NewGlobalRef(bridgeCls));

    JNINativeMethod natives[] = {
        {const_cast<char*>("captureWorldMatricesNative"),const_cast<char*>("()[F"),reinterpret_cast<void*>(Java_br_lock_bridge_NativeBridge_captureWorldMatricesNative)},
        {const_cast<char *>("syncModuleDefinitionNative"),
         const_cast<char *>(
             "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZI[[Ljava/lang/String;)V"),
         reinterpret_cast<void *>(Java_br_lock_bridge_NativeBridge_syncModuleDefinitionNative)},
        {const_cast<char *>("syncModuleState"), const_cast<char *>("(Ljava/lang/String;Z)V"),
         reinterpret_cast<void *>(Java_br_lock_bridge_NativeBridge_syncModuleState)},
        {const_cast<char *>("isGuiOpenNative"), const_cast<char *>("()Z"),
         reinterpret_cast<void *>(Java_br_lock_bridge_NativeBridge_isGuiOpenNative)},
        {const_cast<char *>("setGuiOpenNative"), const_cast<char *>("(Z)V"),
         reinterpret_cast<void *>(Java_br_lock_bridge_NativeBridge_setGuiOpenNative)},
        {const_cast<char *>("onLogNative"), const_cast<char *>("(Ljava/lang/String;)V"),
         reinterpret_cast<void *>(Java_br_lock_bridge_NativeBridge_onLogNative)},
        {const_cast<char *>("sendNotificationNative"),
         const_cast<char *>("(Ljava/lang/String;Ljava/lang/String;)V"),
         reinterpret_cast<void *>(Java_br_lock_bridge_NativeBridge_sendNotificationNative)},
        {const_cast<char *>("updateDamageSnapshotNative"), const_cast<char *>("(Ljava/lang/String;FZ)V"),
         reinterpret_cast<void *>(Java_br_lock_bridge_NativeBridge_updateDamageSnapshotNative)},
        {const_cast<char *>("publishVisualsNative"),
         const_cast<char *>("([Ljava/lang/String;[F[Ljava/lang/String;[FZ)V"),
         reinterpret_cast<void *>(Java_br_lock_bridge_NativeBridge_publishVisualsNative)}};
    if (env->RegisterNatives(g_bridgeCls, natives, sizeof(natives) / sizeof(natives[0])) != JNI_OK) {
        jvm::clearException(env, "RegisterNatives NativeBridge");
    } else {
        logger::info("NativeBridge: Metodos nativos registrados via RegisterNatives com sucesso.");
    }

    g_setModuleEnabledMid = env->GetStaticMethodID(g_bridgeCls, "setModuleEnabled", "(Ljava/lang/String;Z)V");
    g_setSettingValueMid = env->GetStaticMethodID(
        g_bridgeCls, "setSettingValue", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
    g_setKeybindMid = env->GetStaticMethodID(g_bridgeCls, "setModuleKeybind", "(Ljava/lang/String;I)V");
    g_addClanEntryMid =
        env->GetStaticMethodID(g_bridgeCls, "addClanEntry", "(Ljava/lang/String;Ljava/lang/String;)V");
    g_removeClanEntryMid =
        env->GetStaticMethodID(g_bridgeCls, "removeClanEntry", "(Ljava/lang/String;Ljava/lang/String;)V");
    g_saveConfigMid = env->GetStaticMethodID(g_bridgeCls, "triggerSaveConfig", "()V");
    g_loadConfigMid = env->GetStaticMethodID(g_bridgeCls, "triggerLoadConfig", "()V");

    logger::info("NativeBridge vinculado! bridgeCls=%p, setModuleEnabledMid=%p", g_bridgeCls,
                 g_setModuleEnabledMid);
}

static void ensureJavaMethods(JNIEnv *env) {
    if (!g_bridgeCls) {
        jclass localCls = env->FindClass("dev/nebula/client/bridge/NativeBridge");
        if (!localCls) {
            jvm::clearException(env, "FindClass NativeBridge direct");
            jobject gameLoader = jvm::getGameClassLoader();
            if (gameLoader) {
                jclass loaderCls = env->FindClass("java/lang/ClassLoader");
                jmethodID loadMid = loaderCls ? env->GetMethodID(loaderCls, "loadClass",
                                                                 "(Ljava/lang/String;)Ljava/lang/Class;")
                                              : nullptr;
                if (loadMid) {
                    jstring jName = env->NewStringUTF("dev.nebula.client.bridge.NativeBridge");
                    localCls = reinterpret_cast<jclass>(env->CallObjectMethod(gameLoader, loadMid, jName));
                    if (env->ExceptionCheck()) {
                        jvm::clearException(env, "loadClass NativeBridge via gameLoader");
                        localCls = nullptr;
                    }
                    env->DeleteLocalRef(jName);
                }
                if (loaderCls)
                    env->DeleteLocalRef(loaderCls);
            }
        }
        if (localCls) {
            bindJavaBridge(env, localCls);
            env->DeleteLocalRef(localCls);
        } else {
            logger::error("ensureJavaMethods: NativeBridge class not found via any loader");
            return;
        }
    }
}

void setModuleEnabledFromGui(const std::string &name, bool enabled) {
    logger::info("setModuleEnabledFromGui: alterando '%s' -> %s", name.c_str(),
                 enabled ? "ATIVO" : "DESATIVADO");
    {
        ScopedLock lock(&g_bridgeLock);
        g_snapshotDirty = true;
        for (auto &m : g_modules) {
            if (m.name == name) {
                m.enabled = enabled;
                break;
            }
        }
    }

    jvm::ScopedJniEnv env;
    if (!env.isValid()) {
        logger::error("setModuleEnabledFromGui: ScopedJniEnv invalido!");
        return;
    }
    ensureJavaMethods(env.get());

    if (g_bridgeCls && g_setModuleEnabledMid) {
        jstring jName = env->NewStringUTF(name.c_str());
        env->CallStaticVoidMethod(g_bridgeCls, g_setModuleEnabledMid, jName, static_cast<jboolean>(enabled));
        if (env->ExceptionCheck()) {
            jvm::clearException(env.get(), "CallStaticVoidMethod setModuleEnabled");
        } else {
            logger::info("setModuleEnabledFromGui: JNI despachado com sucesso para %s (%d)!", name.c_str(),
                         enabled);
        }
        env->DeleteLocalRef(jName);
    } else {
        logger::error("setModuleEnabledFromGui: g_bridgeCls=%p ou g_setModuleEnabledMid=%p nulos!",
                      g_bridgeCls, g_setModuleEnabledMid);
    }
}

void setSettingValueFromGui(const std::string &moduleName, const std::string &settingName,
                            const std::string &value) {
    {
        ScopedLock lock(&g_bridgeLock);
        g_snapshotDirty = true;
        for (auto &m : g_modules) {
            if (m.name == moduleName) {
                for (auto &s : m.settings) {
                    if (s.name == settingName) {
                        s.value = value;
                        if (s.type == "boolean") {
                            s.boolVal = (value == "true");
                        } else if (s.type == "double" || s.type == "integer") {
                            s.doubleVal = std::stod(value);
                        }
                        break;
                    }
                }
                break;
            }
        }
    }

    jvm::ScopedJniEnv env;
    if (!env.isValid())
        return;
    ensureJavaMethods(env.get());

    if (g_bridgeCls && g_setSettingValueMid) {
        jstring jMod = env->NewStringUTF(moduleName.c_str());
        jstring jSet = env->NewStringUTF(settingName.c_str());
        jstring jVal = env->NewStringUTF(value.c_str());
        env->CallStaticVoidMethod(g_bridgeCls, g_setSettingValueMid, jMod, jSet, jVal);
        jvm::clearException(env.get(), "setSettingValueFromGui");
        env->DeleteLocalRef(jMod);
        env->DeleteLocalRef(jSet);
        env->DeleteLocalRef(jVal);
    }
}

void setKeybindFromGui(const std::string &moduleName, int newKey) {
    {
        ScopedLock lock(&g_bridgeLock);
        g_snapshotDirty = true;
        for (auto &m : g_modules) {
            if (m.name == moduleName) {
                m.key = newKey;
                break;
            }
        }
    }

    jvm::ScopedJniEnv env;
    if (!env.isValid())
        return;
    ensureJavaMethods(env.get());

    if (g_bridgeCls && g_setKeybindMid) {
        jstring jMod = env->NewStringUTF(moduleName.c_str());
        env->CallStaticVoidMethod(g_bridgeCls, g_setKeybindMid, jMod, static_cast<jint>(newKey));
        jvm::clearException(env.get(), "setKeybindFromGui");
        env->DeleteLocalRef(jMod);
    }
}

void addClanFromGui(const std::string &type, const std::string &name) {
    {
        ScopedLock lock(&g_bridgeLock);
        if (type == "allied_clan") {
            g_clanData.alliedClans.push_back(name);
        } else if (type == "enemy_clan") {
            g_clanData.enemyClans.push_back(name);
        } else if (type == "allied_player") {
            g_clanData.alliedPlayers.push_back(name);
        }
    }

    jvm::ScopedJniEnv env;
    if (!env.isValid())
        return;
    ensureJavaMethods(env.get());

    if (g_bridgeCls && g_addClanEntryMid) {
        jstring jType = env->NewStringUTF(type.c_str());
        jstring jName = env->NewStringUTF(name.c_str());
        env->CallStaticVoidMethod(g_bridgeCls, g_addClanEntryMid, jType, jName);
        jvm::clearException(env.get(), "addClanFromGui");
        env->DeleteLocalRef(jType);
        env->DeleteLocalRef(jName);
    }
}

void removeClanFromGui(const std::string &type, const std::string &name) {
    {
        ScopedLock lock(&g_bridgeLock);
        auto removeFn = [&](std::vector<std::string> &vec) {
            vec.erase(std::remove(vec.begin(), vec.end(), name), vec.end());
        };
        if (type == "allied_clan")
            removeFn(g_clanData.alliedClans);
        else if (type == "enemy_clan")
            removeFn(g_clanData.enemyClans);
        else if (type == "allied_player")
            removeFn(g_clanData.alliedPlayers);
    }

    jvm::ScopedJniEnv env;
    if (!env.isValid())
        return;
    ensureJavaMethods(env.get());

    if (g_bridgeCls && g_removeClanEntryMid) {
        jstring jType = env->NewStringUTF(type.c_str());
        jstring jName = env->NewStringUTF(name.c_str());
        env->CallStaticVoidMethod(g_bridgeCls, g_removeClanEntryMid, jType, jName);
        jvm::clearException(env.get(), "removeClanFromGui");
        env->DeleteLocalRef(jType);
        env->DeleteLocalRef(jName);
    }
}

void resetSettingsFromGui(const std::string &name) {
    jvm::ScopedJniEnv env;
    if (!env.isValid() || !g_bridgeCls)
        return;
    jmethodID method = env->GetStaticMethodID(g_bridgeCls, "resetModuleSettings", "(Ljava/lang/String;)V");
    if (jvm::clearException(env.get(), "resetModuleSettings lookup") || !method)
        return;
    jstring module = env->NewStringUTF(name.c_str());
    env->CallStaticVoidMethod(g_bridgeCls, method, module);
    env->DeleteLocalRef(module);
    jvm::clearException(env.get(), "resetModuleSettings");
}

void saveConfigFromGui() {
    jvm::ScopedJniEnv env;
    if (!env.isValid())
        return;
    ensureJavaMethods(env.get());
    if (g_bridgeCls && g_saveConfigMid) {
        env->CallStaticVoidMethod(g_bridgeCls, g_saveConfigMid);
        jvm::clearException(env.get(), "saveConfigFromGui");
    }
}

void loadConfigFromGui() {
    jvm::ScopedJniEnv env;
    if (!env.isValid())
        return;
    ensureJavaMethods(env.get());
    if (g_bridgeCls && g_loadConfigMid) {
        env->CallStaticVoidMethod(g_bridgeCls, g_loadConfigMid);
        jvm::clearException(env.get(), "loadConfigFromGui");
    }
}

void updateModuleStateFromJava(const std::string &name, bool enabled) {
    logger::info("updateModuleStateFromJava: modulo '%s' sincronizado como %s", name.c_str(),
                 enabled ? "ATIVO" : "DESATIVADO");
    ScopedLock lock(&g_bridgeLock);
    g_snapshotDirty = true;
    bool found = false;
    for (auto &m : g_modules) {
        if (m.name == name) {
            m.enabled = enabled;
            found = true;
            break;
        }
    }
    if (!found) {
        ModuleItem newItem;
        newItem.name = name;
        newItem.description = "Modulo Nebula";
        newItem.category = "COMBAT";
        newItem.enabled = enabled;
        g_modules.push_back(newItem);
    }
}

void updateModuleDefinition(ModuleItem module) {
    ScopedLock lock(&g_bridgeLock);
    g_snapshotDirty = true;
    for (auto &existing : g_modules) {
        if (existing.name == module.name) {
            existing = std::move(module);
            return;
        }
    }
    g_modules.push_back(std::move(module));
}

void updateModulesFromJson(const std::string &json) {
    ScopedLock lock(&g_bridgeLock);
}

void updateClanDataFromJson(const std::string &json) {
    ScopedLock lock(&g_bridgeLock);
}

} 
} 

extern "C" {

JNIEXPORT void JNICALL Java_br_lock_bridge_NativeBridge_syncModuleState(JNIEnv *env, jclass, jstring jName,
                                                                        jboolean jEnabled) {
    if (!jName)
        return;
    const char *cName = env->GetStringUTFChars(jName, nullptr);
    if (cName) {
        nebula::bridge::updateModuleStateFromJava(cName, jEnabled == JNI_TRUE);
        env->ReleaseStringUTFChars(jName, cName);
    }
}

JNIEXPORT jboolean JNICALL Java_br_lock_bridge_NativeBridge_isGuiOpenNative(JNIEnv *, jclass) {
    return nebula::bridge::isGuiOpen() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_br_lock_bridge_NativeBridge_setGuiOpenNative(JNIEnv *, jclass, jboolean jOpen) {
    nebula::bridge::setGuiOpen(jOpen == JNI_TRUE);
}

JNIEXPORT void JNICALL Java_br_lock_bridge_NativeBridge_onLogNative(JNIEnv *env, jclass, jstring jMsg) {
    if (!jMsg)
        return;
    const char *cMsg = env->GetStringUTFChars(jMsg, nullptr);
    if (cMsg) {
        nebula::logger::info("[Java] %s", cMsg);
        env->ReleaseStringUTFChars(jMsg, cMsg);
    }
}

JNIEXPORT void JNICALL Java_br_lock_bridge_NativeBridge_syncModuleDefinitionNative(
    JNIEnv *env, jclass, jstring jName, jstring jDescription, jstring jCategory, jboolean enabled, jint key,
    jobjectArray settings) {
    auto stringValue = [env](jstring value) -> std::string {
        if (!value)
            return {};
        const char *chars = env->GetStringUTFChars(value, nullptr);
        if (!chars)
            return {};
        std::string result(chars);
        env->ReleaseStringUTFChars(value, chars);
        return result;
    };
    nebula::bridge::ModuleItem module;
    module.name = stringValue(jName);
    module.description = stringValue(jDescription);
    module.category = stringValue(jCategory);
    module.enabled = enabled == JNI_TRUE;
    module.key = key;
    if (module.name.empty())
        return;
    for (jsize i = 0; settings && i < env->GetArrayLength(settings); ++i) {
        auto row = reinterpret_cast<jobjectArray>(env->GetObjectArrayElement(settings, i));
        if (!row)
            continue;
        std::vector<std::string> fields;
        for (jsize j = 0; j < env->GetArrayLength(row); ++j) {
            auto value = reinterpret_cast<jstring>(env->GetObjectArrayElement(row, j));
            fields.push_back(stringValue(value));
            if (value)
                env->DeleteLocalRef(value);
        }
        env->DeleteLocalRef(row);
        if (fields.size() < 5)
            continue;
        nebula::bridge::SettingItem setting;
        setting.name = fields[0];
        setting.type = fields[1];
        setting.value = fields[2];
        setting.boolVal = fields[2] == "true";
        try {
            setting.minVal = std::stod(fields[3]);
            setting.maxVal = std::stod(fields[4]);
            if (setting.type == "double" || setting.type == "integer")
                setting.doubleVal = std::stod(fields[2]);
            if (setting.type == "keybind")
                setting.keyVal = std::stoi(fields[2]);
        } catch (const std::exception &) {
            continue;
        }
        setting.options.assign(fields.begin() + 5, fields.end());
        module.settings.push_back(std::move(setting));
    }
    nebula::bridge::updateModuleDefinition(std::move(module));
}

JNIEXPORT void JNICALL Java_br_lock_bridge_NativeBridge_sendNotificationNative(JNIEnv *env, jclass,
                                                                               jstring jType,
                                                                               jstring jMessage) {
    if (!jType || !jMessage)
        return;
    const char *type = env->GetStringUTFChars(jType, nullptr);
    const char *message = env->GetStringUTFChars(jMessage, nullptr);
    if (type && message)
        nebula::bridge::pushNotification(type, message);
    if (type)
        env->ReleaseStringUTFChars(jType, type);
    if (message)
        env->ReleaseStringUTFChars(jMessage, message);
}

JNIEXPORT void JNICALL Java_br_lock_bridge_NativeBridge_updateDamageSnapshotNative(JNIEnv *env, jclass,
                                                                                   jstring jName,
                                                                                   jfloat health,
                                                                                   jboolean visible) {
    std::string name;
    if (jName) {
        const char *value = env->GetStringUTFChars(jName, nullptr);
        if (value) {
            name.assign(value);
            env->ReleaseStringUTFChars(jName, value);
        }
    }
    nebula::bridge::updateDamageSnapshot(name, health, visible == JNI_TRUE);
}
}
