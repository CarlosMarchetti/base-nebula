#pragma once

#include <string>
#include <vector>
#include <mutex>
#include <memory>
#include <chrono>
#include <jni.h>
#include "nebula/core/types.hpp"

namespace nebula {
namespace bridge {

struct SettingItem {
    std::string name;
    std::string type; 
    std::string value;
    double doubleVal = 0.0;
    double minVal = 0.0;
    double maxVal = 100.0;
    bool boolVal = false;
    int keyVal = 0;
    std::vector<std::string> options;
};

struct ModuleItem {
    std::string name;
    std::string description;
    std::string category; 
    bool enabled = false;
    int key = 0;
    std::vector<SettingItem> settings;
};

struct ClanData {
    std::vector<std::string> alliedClans;
    std::vector<std::string> enemyClans;
    std::vector<std::string> alliedPlayers;
};

struct NotificationItem {
    std::string type;
    std::string message;
    std::chrono::steady_clock::time_point createdAt;
};

struct DamageSnapshot {
    std::string name;
    float health = 0.f;
    bool visible = false;
};

void init();
void shutdown();

void bindJavaBridge(JNIEnv* env, jclass bridgeCls);

bool isGuiOpen();
void setGuiOpen(bool open);
void toggleGui();
bool isHudEnabled();

std::vector<ModuleItem> getModulesSnapshot();
std::shared_ptr<const std::vector<ModuleItem>> getSharedModulesSnapshot();
ClanData getClanDataSnapshot();
void pushNotification(const std::string& type, const std::string& message);
std::vector<NotificationItem> getNotificationsSnapshot();
void updateDamageSnapshot(const std::string& name, float health, bool visible);
DamageSnapshot getDamageSnapshot();

void setModuleEnabledFromGui(const std::string& name, bool enabled);
void setSettingValueFromGui(const std::string& moduleName, const std::string& settingName, const std::string& value);
void setKeybindFromGui(const std::string& moduleName, int newKey);
void addClanFromGui(const std::string& type, const std::string& name);
void removeClanFromGui(const std::string& type, const std::string& name);
void saveConfigFromGui();
void resetSettingsFromGui(const std::string& moduleName);
void loadConfigFromGui();

void updateModuleStateFromJava(const std::string& name, bool enabled);
void updateModulesFromJson(const std::string& json);
void updateClanDataFromJson(const std::string& json);

} 
} 

