package dev.nebula.client.gui.settings.impl;

import dev.nebula.client.gui.settings.Setting;
import dev.nebula.client.modules.Module;

import java.util.ArrayList;
import java.util.List;

public class ListSetting extends Setting {
    private List<String> value;

    public ListSetting(String name, Module parentModule, List<String> defaultValue) {
        super(name, parentModule, "List");
        this.value = new ArrayList<>(defaultValue);
    }

    @Override
    public synchronized Object getValue() {
        return new ArrayList<>(value);
    }

    @SuppressWarnings("unchecked")
	@Override
    public synchronized void setValue(Object value) {
        if (value instanceof List) {
            for(Object item:(List<?>)value)if(!(item instanceof String))throw new IllegalArgumentException("Expected string list");
            this.value = new ArrayList<>((List<String>) value);
            save();
        }
    }

    public synchronized void addValue(String item) {
        if (!value.contains(item)) {
            value.add(item);
            save();
        }
    }

    public synchronized void removeValue(String item) {
        value.remove(item);
        save();
    }

    public boolean isList() {
        return true;
    }
}
