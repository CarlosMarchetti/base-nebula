package dev.nebula.client.modules.render;

import dev.nebula.client.Main;
import dev.nebula.client.bridge.NativeBridge;
import dev.nebula.client.gui.settings.impl.ComboBoxSetting;
import dev.nebula.client.gui.settings.impl.DoubleSetting;
import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.mappings.wrapper.entity.Entity;
import dev.nebula.client.mappings.wrapper.entity.EntityLivingBase;
import dev.nebula.client.modules.Category;
import dev.nebula.client.modules.Module;

public final class DamageIndicator extends Module {
    private static final String[] MODES = {"J3", "Ultimate", "J3 Ultimate", "Astralis"};

    public DamageIndicator() {
        super("DamageIndicator", "Mostra vida e nome do jogador mirado", Category.RENDER, 0);
    }

    @Override
    public void setup() {
        Main.getSettingsManager().addSetting(new ComboBoxSetting("Mode", this, "J3", MODES));
        Main.getSettingsManager()
                .addSetting(
                        new dev.nebula.client.gui.settings.impl.ColorPickerSetting(
                                "Color", this, 0xFFF28D27, "Astralis"));
        Main.getSettingsManager().addSetting(new DoubleSetting("Scale", this, 1.0, 0.5, 2.0));
        Main.getSettingsManager().addSetting(new DoubleSetting("X", this, 0.75, 0.0, 1.0));
        Main.getSettingsManager().addSetting(new DoubleSetting("Y", this, 0.45, 0.0, 1.0));
    }

    @Override
    public void onEnable() {
        RenderOverlay();
    }

    @Override
    public void onDisable() {
        publishTarget("", 0f, false);
    }

    @Override
    public void onTick() {
        RenderOverlay();
    }

    public void RenderOverlay() {
        Object target = Main.getContext().game().crosshairTarget();
        if (target != null
                && Mappings.ENTITY_PLAYER_CLASS != null
                && !Mappings.ENTITY_PLAYER_CLASS.isInstance(target)) target = null;
        if (target == null) {
            publishTarget("", 0f, false);
            return;
        }
        String name = new Entity(target).getName();
        float health = new EntityLivingBase(target).getHealth();
        publishTarget(name, health, !name.isEmpty());
    }

    public String GetMode() {
        return getValueString("Mode");
    }

    public float GetScale() {
        return (float) getValueDouble("Scale");
    }

    public float GetAnchorX() {
        return (float) getValueDouble("X");
    }

    public float GetAnchorY() {
        return (float) getValueDouble("Y");
    }

    private void publishTarget(String name, float health, boolean visible) {
        try {
            NativeBridge.updateDamageSnapshotNative(name, health, visible);
        } catch (UnsatisfiedLinkError ignored) {
        }
    }
}
