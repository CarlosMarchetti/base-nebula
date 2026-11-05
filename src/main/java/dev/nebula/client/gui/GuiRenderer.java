package dev.nebula.client.gui;

import dev.nebula.client.bridge.NativeBridge;
import java.util.logging.Logger;

public class GuiRenderer {
    private static final Logger LOGGER = Logger.getLogger(GuiRenderer.class.getName());

    public void open() {
        LOGGER.info("Abrindo Nebula Dear ImGui...");
        NativeBridge.setGuiOpen(true);
    }

    public void close() {
        LOGGER.info("Fechando Nebula Dear ImGui...");
        NativeBridge.setGuiOpen(false);
    }

    public boolean isOpen() {
        return NativeBridge.isGuiOpen();
    }
}
