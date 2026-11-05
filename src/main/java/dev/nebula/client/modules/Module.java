package dev.nebula.client.modules;

import dev.nebula.client.Main;
import dev.nebula.client.bridge.NativeBridge;
import dev.nebula.client.connection.Connection;
import dev.nebula.client.gui.settings.Setting;
import dev.nebula.client.modules.render.Notifications;

public abstract class Module {

    private final String name;
    private final String description;
    private volatile boolean enabled;
    private boolean settingsInitialized;
    private final java.util.Map<String,String> defaults = new java.util.LinkedHashMap<>();
    private int key;
    private Category category;

    public Module(String name, String description, Category category, int key) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.key = key;
        this.enabled = false;
    }

    public final synchronized void initializeSettings() {
        if(settingsInitialized)return;
        setup();
        for (Setting setting : Main.getSettingsManager().getSettingsByModule(this))
            defaults.put(setting.getName(), dev.nebula.client.config.SettingCodec.encode(setting));
        settingsInitialized=true;
    }

    public final void restoreDefaultSettings() {
        initializeSettings();
        for (Setting setting : Main.getSettingsManager().getSettingsByModule(this)) {
            String value=defaults.get(setting.getName());
            if(value!=null) dev.nebula.client.config.SettingCodec.apply(setting,value);
        }
    }

    public void setEnabled(boolean enabled) {
        if (enabled) enable();
        else disable();
    }

    public String getName() { return this.name; }
    public int getKey() { return this.key; }
    public void setKey(int key) { this.key = key; }
    public Category getCategory() { return this.category; }
    public void setCategory(Category category) { this.category = category; }
    public boolean isEnabled() { return this.enabled; }
    public String getDescription() { return description; }

    public synchronized void enable() {
        initializeSettings();
        if (this.enabled) return;
        this.enabled = true;
        try {
            this.onEnable();
            NativeBridge.notifyModuleStateChanged(this.name, true);
            Notifications.SendNotifications.ENABLED(this.name);
        } catch (Exception e) {
            dev.nebula.client.utils.Log.error("Erro ao ativar " + this.name + ": " + e.getMessage());
            this.enabled = false;
        }
    }

    public synchronized void disable() {
        if (!this.enabled) return;
        this.enabled = false;
        try {
            this.onDisable();
            NativeBridge.notifyModuleStateChanged(this.name, false);
            Notifications.SendNotifications.DISABLED(this.name);
        } catch (Exception e) {
            dev.nebula.client.utils.Log.error("Erro ao desativar " + this.name + ": " + e.getMessage());
        }
    }

    public synchronized void toggle() {
        if (this.isEnabled()) disable();
        else enable();
    }

    public boolean getHold() { return false; }

    public void setToggled(boolean toggled) {
        setEnabled(toggled);
    }


    public Setting getSetting(String name) {
        return Main.getSettingsManager().getSettingByName(this, name);
    }

    public boolean getValueBoolean(String name) {
        Setting s = getSetting(name);
        return s != null && s.getValue() instanceof Boolean && (Boolean) s.getValue();
    }

    public double getValueDouble(String name) {
        Setting s = getSetting(name);
        if (s == null || !(s.getValue() instanceof Number)) return 0.0;
        return ((Number) s.getValue()).doubleValue();
    }

    public String getValueString(String name) {
        Setting s = getSetting(name);
        return s != null ? s.getValue().toString() : "";
    }


    public abstract void onEnable();
    public abstract void onDisable();
    public void onPlayerMoveUpdate(Object movementInput) throws Exception {}
    public abstract void onTick() throws Exception;
    public void setup() {}
    public boolean onPacket(Object packet, Connection.Side side) { return true; }
}
