package dev.nebula.client.gui.settings.impl;

import dev.nebula.client.gui.settings.Setting;
import dev.nebula.client.modules.Module;

public class RangeSetting extends Setting {
    private double minValue;
    private double maxValue;
    private final double min;
    private final double max;
    private final boolean isDouble;

    public RangeSetting(String name, Module parentModule, double minValue, double maxValue, double min, double max, boolean isDouble) {
        super(name, parentModule, "Range");
        this.minValue = Math.max(min, Math.min(max, minValue));
        this.maxValue = Math.max(min, Math.min(max, maxValue));
        this.min = min;
        this.max = max;
        this.isDouble = isDouble;
    }

    public boolean isDouble() {
        return isDouble;
    }

    public double getMinValue() {
        return minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public void setMinValue(double value) {
        this.minValue = Math.max(min, Math.min(maxValue, value));
        save();
    }

    public void setMaxValue(double value) {
        this.maxValue = Math.max(minValue, Math.min(max, value));
        save();
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    @Override
    public Object getValue() {
        if (isDouble) {
            return new double[]{minValue, maxValue};
        } else {
            return new int[]{(int) minValue, (int) maxValue};
        }
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof double[]) {
            double[] doubles = (double[]) value;
            this.minValue = Math.max(min, Math.min(max, doubles[0]));
            this.maxValue = Math.max(this.minValue, Math.min(max, doubles[1]));
        } else if (value instanceof int[]) {
            int[] ints = (int[]) value;
            this.minValue = Math.max(min, Math.min(max, ints[0]));
            this.maxValue = Math.max(this.minValue, Math.min(max, ints[1]));
        }
        save();
    }

    public boolean isRange() {
        return true;
    }
}
