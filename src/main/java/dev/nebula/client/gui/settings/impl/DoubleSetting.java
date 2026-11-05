package dev.nebula.client.gui.settings.impl;

import dev.nebula.client.gui.settings.Setting;
import dev.nebula.client.modules.Module;

public class DoubleSetting extends Setting {
    private double value;
    private final double min;
    private final double max;

    public DoubleSetting(String name, Module parentModule, double value, double min, double max) {
        super(name, parentModule, "Double");
        this.value = value;
        this.min = min;
        this.max = max;
    }

    @Override
    public Double getValue() {
        return value;
    }

    @Override
    public void setValue(Object value) {
        this.value = Math.max(min, Math.min(max, (Double) value));
        save();
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }
}
