package dev.nebula.client.bridge;

import dev.nebula.client.Main;
import dev.nebula.client.config.SettingCodec;
import dev.nebula.client.gui.settings.Setting;
import dev.nebula.client.managers.ClanManager;
import dev.nebula.client.modules.Module;
import dev.nebula.client.modules.ModuleManager;
import java.util.logging.Logger;

public final class NativeBridge {

    private static final Logger LOGGER = Logger.getLogger(NativeBridge.class.getName());

    private NativeBridge() {}

    public static void init() {
        log("NativeBridge Java inicializado com sucesso.");
        syncModules();

        try {
            ModuleManager mm = Main.getModuleManager();
            if (mm != null) {
                for (Module m : mm.getAll()) {
                    notifyModuleStateChanged(m.getName(), m.isEnabled());
                }
                log("Sincronizados " + mm.getAll().size() + " modulos com C++.");
            }
        } catch (Throwable t) {
            LOGGER.warning("Falha ao sincronizar modulos no init: " + t.getMessage());
        }
    }

    public static void syncModules() {
        if (Main.getModuleManager() == null || Main.getSettingsManager() == null) return;
        try {
            for (Module module : Main.getModuleManager().getAll()) {
                java.util.List<String[]> rows = new java.util.ArrayList<>();
                for (Setting setting : Main.getSettingsManager().getSettingsByModule(module)) {
                    rows.add(SettingCodec.describe(setting));
                }
                syncModuleDefinitionNative(
                        module.getName(),
                        module.getDescription(),
                        module.getCategory().name(),
                        module.isEnabled(),
                        module.getKey(),
                        rows.toArray(new String[0][]));
            }
        } catch (UnsatisfiedLinkError ignored) {
        } catch (RuntimeException e) {
            log("Module settings sync failed: " + e);
        }
    }


    public static void setModuleEnabled(String moduleName, boolean enabled) {
        if (moduleName.equalsIgnoreCase("ClickGui") && Main.getContext() != null)
            Main.getContext().guiInput().request(enabled);
        Main.executeOnClientThread(() -> setModuleEnabledOnClientThread(moduleName, enabled));
    }

    private static void setModuleEnabledOnClientThread(String moduleName, boolean enabled) {
        try {
            ModuleManager mm = Main.getModuleManager();
            if (mm == null) {
                return;
            }
            Module mod = mm.getModule(moduleName);
            if (mod != null) {
                if (enabled && !mod.isEnabled()) {
                    mod.enable();
                } else if (!enabled && mod.isEnabled()) {
                    mod.disable();
                }
                Main.getSaveLoad().requestSave();
                syncModules();
            }
        } catch (Throwable t) {
            LOGGER.severe("Erro ao setModuleEnabled: " + t.getMessage());
        }
    }

    public static void setSettingValue(String moduleName, String settingName, String value) {
        Main.executeOnClientThread(
                () -> setSettingValueOnClientThread(moduleName, settingName, value));
    }

    private static void setSettingValueOnClientThread(
            String moduleName, String settingName, String value) {
        try {
            if (Main.getSettingsManager() == null) return;
            for (Setting s : Main.getSettingsManager().getSettings()) {
                if (s != null
                        && s.getParentModule() != null
                        && s.getParentModule().getName().equalsIgnoreCase(moduleName)
                        && s.getName().equalsIgnoreCase(settingName)) {

                    SettingCodec.apply(s, value);
                    syncModules();
                    break;
                }
            }
        } catch (Throwable t) {
            LOGGER.severe("Erro ao setSettingValue: " + t.getMessage());
        }
    }

    public static void resetModuleSettings(String moduleName) {
        Main.executeOnClientThread(
                () -> {
                    Module module = Main.getModuleManager().getModule(moduleName);
                    if (module == null) return;
                    module.restoreDefaultSettings();
                    Main.getSaveLoad().requestSave();
                    syncModules();
                });
    }

    public static void setModuleKeybind(String moduleName, int newKey) {
        Main.executeOnClientThread(() -> setModuleKeybindOnClientThread(moduleName, newKey));
    }

    private static void setModuleKeybindOnClientThread(String moduleName, int newKey) {
        try {
            ModuleManager mm = Main.getModuleManager();
            if (mm == null) return;
            Module mod = mm.getModule(moduleName);
            if (mod != null) {
                mod.setKey(newKey);
                Main.getSaveLoad().requestSave();
                syncModules();
            }
        } catch (Throwable t) {
            LOGGER.severe("Erro ao setModuleKeybind: " + t.getMessage());
        }
    }

    public static void addClanEntry(String type, String name) {
        Main.executeOnClientThread(() -> addClanEntryOnClientThread(type, name));
    }

    public static void removeClanEntry(String type, String name) {
        Main.executeOnClientThread(() -> removeClanEntryOnClientThread(type, name));
    }

    private static void addClanEntryOnClientThread(String type, String name) {
        ClanManager cm = Main.getClanManager();
        if (cm == null || name == null || name.trim().isEmpty()) return;
        name = name.trim();
        if ("allied_clan".equalsIgnoreCase(type)) {
            cm.addAlliedClan(name);
        } else if ("enemy_clan".equalsIgnoreCase(type)) {
            cm.addEnemyClan(name);
        } else if ("allied_player".equalsIgnoreCase(type)) {
            cm.addAlliedPlayer(name);
        }
    }

    private static void removeClanEntryOnClientThread(String type, String name) {
        ClanManager cm = Main.getClanManager();
        if (cm == null || name == null) return;
        if ("allied_clan".equalsIgnoreCase(type)) {
            cm.removeAlliedClan(name);
        } else if ("enemy_clan".equalsIgnoreCase(type)) {
            cm.removeEnemyClan(name);
        } else if ("allied_player".equalsIgnoreCase(type)) {
            cm.removeAlliedPlayer(name);
        }
    }

    public static void triggerSaveConfig() {
        Main.executeOnClientThread(
                () -> {
                    try {
                        if (Main.getSaveLoad() != null) {
                            Main.getSaveLoad().save();
                            log("Configuracao salva via Dear ImGui.");
                        }
                    } catch (Throwable t) {
                        LOGGER.severe("Erro ao triggerSaveConfig: " + t.getMessage());
                    }
                });
    }

    public static void triggerLoadConfig() {
        Main.executeOnClientThread(
                () -> {
                    try {
                        if (Main.getSaveLoad() != null) {
                            Main.getSaveLoad().load();
                            syncModules();
                            log("Configuracao recarregada via Dear ImGui.");
                        }
                    } catch (Throwable t) {
                        LOGGER.severe("Erro ao triggerLoadConfig: " + t.getMessage());
                    }
                });
    }


    public static boolean isGuiOpen() {
        try {
            return isGuiOpenNative();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    public static void setGuiOpen(boolean open) {
        try {
            setGuiOpenNative(open);
        } catch (UnsatisfiedLinkError ignored) {
        }
    }

    public static void log(String msg) {
        LOGGER.info("[Nebula] " + msg);
        try {
            onLogNative(msg);
        } catch (UnsatisfiedLinkError ignored) {
        }
    }


    public static void notifyModuleStateChanged(String moduleName, boolean enabled) {
        try {
            syncModuleState(moduleName, enabled);
        } catch (UnsatisfiedLinkError ignored) {
        }
    }


    public static native float[] captureWorldMatricesNative();

    public static native void publishVisualsNative(
            String[] players,
            float[] playerData,
            String[] items,
            float[] itemData,
            boolean projected);

    public static native void syncModuleDefinitionNative(
            String name,
            String description,
            String category,
            boolean enabled,
            int key,
            String[][] settings);

    public static native void syncModuleState(String name, boolean enabled);

    public static native boolean isGuiOpenNative();

    public static native void setGuiOpenNative(boolean open);

    public static native void onLogNative(String message);

    public static native void sendNotificationNative(String type, String message);

    public static native void updateDamageSnapshotNative(
            String name, float health, boolean visible);
}
