package dev.nebula.client.gui.settings.impl;

import dev.nebula.client.gui.settings.Setting;
import dev.nebula.client.modules.Module;

public class BooleanSetting extends Setting {
    private boolean value;

    public BooleanSetting(String name, Module parentModule, boolean value) {
        super(name, parentModule, "Boolean");
        this.value = value;
    }

    @Override
    public Boolean getValue() {
        return value;
    }
    
    public boolean getValBoolean() {
        return this.value;
    }

    
    @Override
    public void setValue(Object value) {
        this.value = (Boolean) value;
        save();
    }
}
