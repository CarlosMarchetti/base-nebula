package dev.nebula.client.modules;

import dev.nebula.client.Main;
import dev.nebula.client.game.*;
import dev.nebula.client.gui.settings.Setting;
import dev.nebula.client.gui.settings.impl.*;
import dev.nebula.client.modules.render.Notifications;
import java.util.*;

public abstract class GameModule extends Module {
    protected GameModule(String name, String description, Category category) {
        super(name, description, category, 0);
    }

    protected final GameAccess game() {
        return Main.getContext().game();
    }

    protected final InventoryLease lease() {
        return Main.getContext().inventoryLease();
    }

    protected final void add(Setting option) {
        Main.getSettingsManager().addSetting(option);
    }

    protected final void bool(String name, boolean value) {
        add(new BooleanSetting(name, this, value));
    }

    protected final void integer(String name, int value, int min, int max) {
        add(new IntSetting(name, this, value, min, max));
    }

    protected final void decimal(String name, double value, double min, double max) {
        add(new DoubleSetting(name, this, value, min, max));
    }

    protected final void choice(String name, String value, String... options) {
        add(new ComboBoxSetting(name, this, value, options));
    }

    protected final void strings(String name) {
        add(new ListSetting(name, this, Collections.emptyList()));
    }

    protected final void strings(String name, String... values) {
        add(new ListSetting(name, this, Arrays.asList(values)));
    }

    protected final void keybind() {
        add(new KeyBindSetting("Keybind", this, getKey()));
    }

    protected final int count(String name) {
        return (int) getValueDouble(name);
    }

    @SuppressWarnings("unchecked")
    protected final List<String> stringsValue(String name) {
        return (List<String>) getSetting(name).getValue();
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {
        lease().release(this);
    }

    @Override
    public final void onTick() {
        try {
            if (game().inGame()) tick();
            else worldLost();
        } catch (MappedAccess.Missing e) {
            try {
                disable();
            } finally {
                lease().release(this);
            }
            Notifications.SendNotifications.ERROR(getName() + ": " + e.getMessage());
        }
    }

    public final void resetWorld() {
        worldLost();
    }

    protected void worldLost() {
        lease().release(this);
    }

    protected abstract void tick();
}
