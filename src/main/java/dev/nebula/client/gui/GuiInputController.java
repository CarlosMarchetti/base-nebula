package dev.nebula.client.gui;

import dev.nebula.client.bridge.NativeBridge;
import dev.nebula.client.game.GameAccess;

public final class GuiInputController {
    private final GameAccess game;
    private volatile boolean requested, captured;

    public GuiInputController(GameAccess game) {
        this.game = game;
    }

    public void request(boolean open) {
        requested = open;
    }

    public boolean blocksCamera(Object entity) {
        return (requested || captured) && entity != null && entity == game.player();
    }

    public boolean isCaptured() {
        return captured;
    }

    public void apply() {
        if (captured && requested && !game.menuCaptured()) {
            requested = false;
            NativeBridge.setGuiOpen(false);
        }
        if (requested && !captured) {
            game.setMenuCapture(true);
            captured = true;
        } else if (!requested && captured) {
            try {
                game.setMenuCapture(false);
            } finally {
                captured = false;
            }
        }
    }
}
