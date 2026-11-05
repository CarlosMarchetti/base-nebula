package dev.nebula.client.mappings.wrapper.network.play.client;

import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.mappings.wrapper.network.Packet;
import dev.nebula.client.utils.ReflectionUtils;

import java.lang.reflect.Constructor;

public class C0EPacketClickWindow extends Packet {

    public C0EPacketClickWindow(int windowId, int slot, int mouseButton, int clickType, Object stack, short actionNumber) {
        super(construct(windowId, slot, mouseButton, clickType, stack, actionNumber));
    }

    private static Object construct(int windowId, int slot, int mouseButton, int clickType, Object stack, short actionNumber) {
        try {
            Constructor<?> ctor = (Constructor<?>) Mappings.C0E_PACKET_CLICK_WINDOW_CONSTRUCTOR;
            return ReflectionUtils.invokeConstructor(ctor,
                    windowId, slot, mouseButton, clickType, stack, actionNumber);
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct C0EPacketClickWindow", e);
        }
    }
}
