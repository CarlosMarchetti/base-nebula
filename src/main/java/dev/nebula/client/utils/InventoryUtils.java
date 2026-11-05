package dev.nebula.client.utils;

import dev.nebula.client.mappings.wrapper.client.Minecraft;
import dev.nebula.client.mappings.wrapper.client.network.NetHandlerPlayClient;
import dev.nebula.client.mappings.wrapper.network.play.client.C0EPacketClickWindow;

public class InventoryUtils {

    private static short transactionId = 0;

    public static void sendClickWindowPacket(int windowId, int slot, int mouseButton, int clickType, Object stack) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            NetHandlerPlayClient net = mc.getNetHandler();

            if (net == null) return;

            C0EPacketClickWindow packet = new C0EPacketClickWindow(
                    windowId,
                    slot,
                    mouseButton,
                    clickType,
                    stack,
                    transactionId++
            );

            net.sendPacket(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
