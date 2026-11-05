package dev.nebula.client.gui.settings;

import dev.nebula.client.Main;
import dev.nebula.client.modules.Module;

public abstract class Setting {
    protected final String name;
    protected final Module parentModule;
    protected final String type;

    public Setting(String name, Module parentModule, String type) {
        this.name = name;
        this.parentModule = parentModule;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Module getParentModule() {
        return parentModule;
    }

    public String getType() {
        return type;
    }

    public abstract Object getValue();

    public abstract void setValue(Object value);

    public boolean isSlider() {
        return type.equals("Int") || type.equals("Double") || type.equals("Float");
    }

    public boolean isCheck() {
        return type.equals("Boolean");
    }

    public boolean isCombo() {
        return type.equals("ComboBox");
    }

    protected void save() {
        if (Main.getSaveLoad() != null) {
            Main.getSaveLoad().requestSave();
        }
    }
}
