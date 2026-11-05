package dev.nebula.client.mappings.wrapper.network.play.client;

import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.utils.ReflectionUtils;

import java.lang.reflect.Constructor;

public class C05PacketPlayerLook extends C03PacketPlayer {

    public C05PacketPlayerLook(float yaw, float pitch, boolean onGround) {
        super(constructLook(yaw, pitch, onGround));
    }

    private static Object constructLook(float yaw, float pitch, boolean onGround) {
        try {
            Constructor<?> ctor = (Constructor<?>) Mappings.C05_PACKET_PLAYER_LOOK_CONSTRUCTOR;
            if (ctor != null) {
                return ReflectionUtils.invokeConstructor(ctor, yaw, pitch, onGround);
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> lookCls = ReflectionUtils.getClass("net.minecraft.network.play.client.C03PacketPlayer$C05PacketPlayerLook");
            Constructor<?> fallbackCtor = lookCls.getConstructor(float.class, float.class, boolean.class);
            fallbackCtor.setAccessible(true);
            return fallbackCtor.newInstance(yaw, pitch, onGround);
        } catch (Throwable t) {
            return new Object();
        }
    }
}
