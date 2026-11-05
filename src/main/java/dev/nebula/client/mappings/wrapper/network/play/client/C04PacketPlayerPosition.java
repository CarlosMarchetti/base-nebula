package dev.nebula.client.mappings.wrapper.network.play.client;

import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.mappings.wrapper.network.Packet;
import dev.nebula.client.utils.ReflectionUtils;

import java.lang.reflect.Constructor;

public class C04PacketPlayerPosition extends Packet {

    public C04PacketPlayerPosition(double x, double y, double z, boolean onGround) {
        super(construct(x, y, z, onGround));
    }

    private static Object construct(double x, double y, double z, boolean onGround) {
        try {
            Constructor<?> ctor = (Constructor<?>) Mappings.C04_PACKET_PLAYER_POSITION_CONSTRUCTOR;
            return ReflectionUtils.invokeConstructor(ctor, x, y, z, onGround);
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct C04PacketPlayerPosition", e);
        }
    }
}
