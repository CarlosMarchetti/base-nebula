package dev.nebula.client.modules.render;

import dev.nebula.client.Main;
import dev.nebula.client.bridge.NativeBridge;
import dev.nebula.client.gui.settings.impl.KeyBindSetting;
import dev.nebula.client.modules.Category;
import dev.nebula.client.modules.Module;
import org.lwjgl.input.Keyboard;

public class ClickGui extends Module {

    public ClickGui() {
        super("ClickGui", "Dear ImGui In-Game Menu", Category.RENDER, Keyboard.KEY_INSERT);
    }

    @Override
    public void setup() {
        Main.getSettingsManager().addSetting(new KeyBindSetting("Keybind", this, this.getKey()));
    }

    @Override
    public void onEnable() {
        Main.getContext().guiInput().request(true);
        NativeBridge.setGuiOpen(true);
    }

    @Override
    public void onDisable() {
        Main.getContext().guiInput().request(false);
        NativeBridge.setGuiOpen(false);
    }

    @Override
    public void onTick() throws Exception {}
}
