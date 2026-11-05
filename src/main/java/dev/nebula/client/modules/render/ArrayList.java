package dev.nebula.client.modules.render;

import dev.nebula.client.Main;
import dev.nebula.client.gui.settings.impl.BooleanSetting;
import dev.nebula.client.gui.settings.impl.ComboBoxSetting;
import dev.nebula.client.gui.settings.impl.ColorPickerSetting;
import dev.nebula.client.modules.Category;
import dev.nebula.client.modules.Module;

public final class ArrayList extends Module {
    private static final String[] MODES = {"Default", "Rise", "Tesseract", "VapeV4"};

    public ArrayList() {
        super("ArrayList", "Lista de módulos ativos na HUD", Category.RENDER, 0);
    }

    @Override
    public void setup() {
        Main.getSettingsManager().addSetting(new ComboBoxSetting("Mode", this, "Default", MODES));
        Main.getSettingsManager().addSetting(new BooleanSetting("Watermark", this, true));
        Main.getSettingsManager().addSetting(new BooleanSetting("Spaced Modules", this, false));
        Main.getSettingsManager().addSetting(new BooleanSetting("Wave", this, false));
        Main.getSettingsManager().addSetting(new ColorPickerSetting("Primary Color", this, 0xFFEB4567, "Primary"));
        Main.getSettingsManager().addSetting(new ColorPickerSetting("Secondary Color", this, 0xFF4AD4E8, "Secondary"));
    }

    @Override public void onEnable() {}
    @Override public void onDisable() {}
    @Override public void onTick() {}
}
