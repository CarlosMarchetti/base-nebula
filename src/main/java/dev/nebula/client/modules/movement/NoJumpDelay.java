package dev.nebula.client.modules.movement;

import dev.nebula.client.modules.*;

public final class NoJumpDelay extends GameModule {
    public NoJumpDelay() {
        super("NoJumpDelay", "Remove o intervalo local entre pulos", Category.MOVEMENT);
    }

    public void setup() {
        keybind();
    }

    protected void tick() {
        if (!game().guiOpen()) game().clearJumpDelay();
    }
}
