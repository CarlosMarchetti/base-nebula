package dev.nebula.client.mappings.wrapper;

import dev.nebula.client.mappings.wrapper.client.Minecraft;
import dev.nebula.client.mappings.wrapper.client.entity.EntityPlayerSP;
import dev.nebula.client.utils.Log;

public class Wrapper {

    private static long lastNullWarn = 0;

    public static boolean isNull() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.getRaw() == null) {
                logNull("MinecraftInstance");
                return true;
            }

            EntityPlayerSP player = mc.getThePlayer();
            if (player == null || player.getHandle() == null) {
                logNull("ThePlayer");
                return true;
            }

            return false;
        } catch (Throwable e) {
            logNull("Exception: " + e.getMessage());
            return true;
        }
    }

    private static void logNull(String reason) {
        long now = System.currentTimeMillis();
        if (now - lastNullWarn >= 4000) {
            lastNullWarn = now;
            Log.warn("[Wrapper] isNull=true motivo: " + reason);
        }
    }

    public static Minecraft getMinecraft() {
        return Minecraft.getMinecraft();
    }
}
