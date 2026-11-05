package dev.nebula.client.gui.settings;

import dev.nebula.client.modules.Module;
import java.util.ArrayList;

public class SettingsManager {
    private final ArrayList<Setting> settings;
    private final java.util.Map<Module, java.util.Map<String, Setting>> byModule =
            new java.util.IdentityHashMap<>();

    public SettingsManager() {
        settings = new ArrayList<>();
    }

    public synchronized void addSetting(Setting setting) {
        if (setting == null) {
            return;
        }
        java.util.Map<String, Setting> named =
                byModule.computeIfAbsent(
                        setting.getParentModule(), key -> new java.util.LinkedHashMap<>());
        if (named.putIfAbsent(setting.getName().toLowerCase(java.util.Locale.ROOT), setting)
                != null)
            throw new IllegalArgumentException("Duplicate setting: " + setting.getName());
        settings.add(setting);
    }

    public synchronized ArrayList<Setting> getSettings() {
        return new ArrayList<>(settings);
    }

    public synchronized ArrayList<Setting> getSettingsByModule(Module module) {
        if (module == null) return new ArrayList<>();
        java.util.Map<String, Setting> named = byModule.get(module);
        return named == null ? new ArrayList<>() : new ArrayList<>(named.values());
    }

    public synchronized Setting getSettingByName(Module module, String name) {
        if (module == null || name == null) return null;
        java.util.Map<String, Setting> named = byModule.get(module);
        return named == null || name == null
                ? null
                : named.get(name.toLowerCase(java.util.Locale.ROOT));
    }
}
