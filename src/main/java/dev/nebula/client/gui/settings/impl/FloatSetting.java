package dev.nebula.client.gui.settings.impl;

import dev.nebula.client.gui.settings.Setting;
import dev.nebula.client.modules.Module;

public class FloatSetting extends Setting {
    private float value;
    private final float min;
    private final float max;

    public FloatSetting(String name, Module parentModule, float value, float min, float max) {
        super(name, parentModule, "Float");
        this.value = value;
        this.min = min;
        this.max = max;
    }

    @Override
    public Float getValue() {
        return value;
    }

    @Override
    public void setValue(Object value) {
        this.value = Math.max(min, Math.min(max, (Float) value));
        save();
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }
}
