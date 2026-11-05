package dev.nebula.client.modules.render;

import dev.nebula.client.Main;
import dev.nebula.client.bridge.NativeBridge;
import dev.nebula.client.modules.Category;
import dev.nebula.client.modules.Module;

public final class Notifications extends Module {

    public Notifications() {
        super("Notifications", "Mensagens do client na HUD", Category.RENDER, 0);
    }

    public static final class SendNotifications {
        private SendNotifications() {}

        public static void ERROR(String message) { send("ERROR", message); }
        public static void INFO(String message) { send("INFO", message); }
        public static void ENABLED(String module) { send("ENABLED", module + " ativado"); }
        public static void DISABLED(String module) { send("DISABLED", module + " desativado"); }
        public static void SUCCESS(String message) { send("SUCCESS", message); }

        private static void send(String type, String message) {
            if (message == null || message.trim().isEmpty()) return;
            Main.executeOnClientThread(() -> {
                if (!isEnabled()) return;
                try {
                    NativeBridge.sendNotificationNative(type, message.trim());
                } catch (UnsatisfiedLinkError ignored) {
                }
            });
        }

        private static boolean isEnabled() {
            return Main.getModuleManager() != null
                    && Main.getModuleManager().getModule("Notifications") != null
                    && Main.getModuleManager().getModule("Notifications").isEnabled();
        }
    }

    @Override public void onEnable() {}
    @Override public void onDisable() {}
    @Override public void onTick() {}
}
