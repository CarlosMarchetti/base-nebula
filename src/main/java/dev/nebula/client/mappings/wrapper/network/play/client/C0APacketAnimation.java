package dev.nebula.client.mappings.wrapper.network.play.client;

import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.mappings.wrapper.network.Packet;
import dev.nebula.client.utils.ReflectionUtils;

import java.lang.reflect.Constructor;

public class C0APacketAnimation extends Packet {

    public C0APacketAnimation() {
        super(construct());
    }

    private static Object construct() {
        try {
            if (Mappings.C0A_PACKET_ANIMATION_CONSTRUCTOR != null) {
                return ReflectionUtils.invokeConstructor((Constructor<?>) Mappings.C0A_PACKET_ANIMATION_CONSTRUCTOR);
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> cls = ReflectionUtils.getClass("net.minecraft.network.play.client.C0APacketAnimation");
            Constructor<?> ctor = cls.getConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Throwable t) {
            return new Object();
        }
    }
}
