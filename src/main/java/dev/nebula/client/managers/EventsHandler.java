package dev.nebula.client.managers;

import dev.nebula.client.Main;
import dev.nebula.client.connection.Connection;
import dev.nebula.client.input.KeyCapture;
import dev.nebula.client.mappings.wrapper.Wrapper;
import dev.nebula.client.modules.Module;
import dev.nebula.client.utils.Log;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class EventsHandler {

    private boolean initialized = false;
    private final java.util.Set<Module> packetFailures =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private final boolean[] keyStates = new boolean[Keyboard.KEYBOARD_SIZE];
    private final boolean[] mouseStates = new boolean[16];

    private Connection connection;
    private KeyCapture activeKeyBindListener;
    private long lastTickLog = 0;

    public void setKeyBindListener(KeyCapture listener) {
        if (activeKeyBindListener != null
                && activeKeyBindListener != listener
                && activeKeyBindListener.isListening()) {
            activeKeyBindListener.stopListening();
        }

        activeKeyBindListener = listener;
        Log.debug("KeyBind listener ativo: " + (listener != null ? listener.getName() : "null"));
    }

    public void onClientTick() {
        if (Wrapper.isNull()) {
            initialized = false;
            return;
        }

        try {
            if (!initialized) {
                try {
                    if (connection == null || !connection.isAttached())
                        connection = Connection.connect(this);
                } catch (Throwable t) {
                    Log.warn("Falha ao injetar Connection no pipeline: " + t.getMessage());
                }
                initialized = connection != null && connection.isAttached();
                Log.info("EventsHandler iniciado com sucesso.");
            }

            int enabledCount = 0;
            for (Module m : Main.getModuleManager().getAll()) {
                if (m.isEnabled()) {
                    enabledCount++;
                    try {
                        m.onTick();
                    } catch (Throwable t) {
                        Log.error(
                                "Erro no onTick do modulo " + m.getName() + ": " + t.getMessage());
                    }
                }
            }

            if (activeKeyBindListener != null && activeKeyBindListener.isListening()) return;

            if (Keyboard.isCreated()) processKeyboard();
            if (Mouse.isCreated()) processMouse();

        } catch (Exception e) {
            Log.error("Erro no ClientTick: " + e.getMessage());
        }
    }

    private void processKeyboard() {
        for (int key = 1; key < keyStates.length; key++) {

            boolean down = Keyboard.isKeyDown(key);

            if (down && !keyStates[key]) {
                keyStates[key] = true;
                handlePress(key);
            } else if (!down && keyStates[key]) {
                keyStates[key] = false;
                handleRelease(key);
            }
        }
    }

    private void processMouse() {
        int count = Mouse.getButtonCount();
        for (int button = 0; button < Math.min(count, mouseStates.length); button++) {

            boolean down = Mouse.isButtonDown(button);
            int mapped = -(button + 2);

            if (down && !mouseStates[button]) {
                mouseStates[button] = true;
                handlePress(mapped);
            } else if (!down && mouseStates[button]) {
                mouseStates[button] = false;
                handleRelease(mapped);
            }
        }
    }

    private void handlePress(int key) {
        if (Wrapper.isNull()) return;
        if (key == Keyboard.KEY_INSERT
                || dev.nebula.client.bridge.NativeBridge.isGuiOpen()
                || !dev.nebula.client.mappings.wrapper.client.Minecraft.inGameHasFocus()) return;

        for (Module m : Main.getModuleManager().getAll()) {
            if (m.getKey() != key) continue;

            if (m.getHold()) {
                if (!m.isEnabled()) m.enable();
            } else {
                m.toggle();
            }
            break;
        }
    }

    private void handleRelease(int key) {
        if (Wrapper.isNull()) return;

        for (Module m : Main.getModuleManager().getAll()) {
            if (m.getKey() == key && m.getHold()) {
                m.disable();
                break;
            }
        }
    }

    public boolean onPacket(Object packet, Connection.Side side) {
        boolean ok = true;

        for (Module m : Main.getModuleManager().getAll()) {
            if (!m.isEnabled()) continue;
            if (packetFailures.contains(m)) continue;
            try {
                ok &= m.onPacket(packet, side);
            } catch (RuntimeException | LinkageError failure) {
                if (packetFailures.add(m)) {
                    Log.error("Packet callback failed: " + m.getName() + ": " + failure);
                    Main.executeOnClientThread(
                            () -> {
                                try {
                                    m.disable();
                                    dev.nebula.client.modules.render.Notifications.SendNotifications.ERROR(
                                            m.getName() + ": falha no callback de pacote");
                                } finally {
                                    packetFailures.remove(m);
                                }
                            });
                }
            }
        }

        return ok;
    }

    public Connection getConnection() {
        return connection;
    }
}
