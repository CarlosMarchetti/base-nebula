package dev.nebula.client.gui.settings.impl;

import dev.nebula.client.gui.settings.Setting;
import dev.nebula.client.modules.Module;

public class ColorPickerSetting extends Setting {
    private int value; 
    private final String colorType; 

    public ColorPickerSetting(String name, Module module, int defaultValue, String colorType) {
        super(name, module, "ColorPicker");
        this.value = defaultValue;
        this.colorType = colorType;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = (int) value;
        save();
    }

    public String getColorType() {
        return colorType;
    }

    @Override
    public boolean isSlider() {
        return false;
    }
}
