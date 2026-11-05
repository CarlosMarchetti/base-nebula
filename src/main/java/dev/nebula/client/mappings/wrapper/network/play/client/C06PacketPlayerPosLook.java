package dev.nebula.client.mappings.wrapper.network.play.client;

import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.mappings.wrapper.network.Packet;
import dev.nebula.client.utils.ReflectionUtils;

import java.lang.reflect.Constructor;

public class C06PacketPlayerPosLook extends Packet {

    public C06PacketPlayerPosLook(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        super(construct(x, y, z, yaw, pitch, onGround));
    }

    private static Object construct(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        try {
            Constructor<?> ctor = (Constructor<?>) Mappings.C06_PACKET_PLAYER_POS_LOOK_CONSTRUCTOR;
            return ReflectionUtils.invokeConstructor(ctor, x, y, z, yaw, pitch, onGround);
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct C06PacketPlayerPosLook", e);
        }
    }
}
