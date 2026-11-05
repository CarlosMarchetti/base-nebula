package dev.nebula.client.modules.combat;

import dev.nebula.client.modules.*;

public final class NoHitDelay extends GameModule {
    public NoHitDelay() {
        super("NoHitDelay", "Remove o intervalo local entre cliques", Category.COMBAT);
    }

    public void setup() {
        keybind();
    }

    protected void tick() {
        if (!game().guiOpen()) game().clearHitDelay();
    }
}
