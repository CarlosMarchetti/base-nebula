package dev.nebula.client.gui.settings.impl;

import dev.nebula.client.gui.settings.Setting;
import dev.nebula.client.modules.Module;

public class IntSetting extends Setting {
    private int value;
    private final int min;
    private final int max;

    public IntSetting(String name, Module parentModule, int value, int min, int max) {
        super(name, parentModule, "Int");
        this.value = value;
        this.min = min;
        this.max = max;
    }

    @Override
    public Integer getValue() {
        return value;
    }

    @Override
    public void setValue(Object value) {
        this.value = Math.max(min, Math.min(max, (Integer) value));
        save();
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }
}
