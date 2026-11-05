package dev.nebula.client.gui.settings.impl;

import dev.nebula.client.gui.settings.Setting;
import dev.nebula.client.modules.Module;

public class ComboBoxSetting extends Setting {
    private String value;
    private String[] options;

    public ComboBoxSetting(String name, Module parentModule, String value, String[] options) {
        super(name, parentModule, "ComboBox");
        this.value = value;
        this.options = options;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof String) {
            String newValue = (String) value;
            for (String option : options) {
                if (option.equals(newValue)) {
                    this.value = newValue;
                    save();
                    break;
                }
            }
        }
    }

    public String[] getOptions() {
        return options;
    }
}
