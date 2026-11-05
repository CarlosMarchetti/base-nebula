package dev.nebula.client.mappings.wrapper.network.play.client;

import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.mappings.wrapper.network.Packet;
import dev.nebula.client.utils.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public class C03PacketPlayer extends Packet {

    public C03PacketPlayer(Object handle) {
        super(handle);
    }

    public C03PacketPlayer() {
        super(construct());
    }

    private static Object construct() {
        try {
            Constructor<?> ctor = (Constructor<?>) Mappings.C03_PACKET_PLAYER_NOARGS_CONSTRUCTOR;
            return ReflectionUtils.invokeConstructor(ctor);
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct C04PacketPlayerPosition", e);
        }
    }
    
    public C03PacketPlayer(boolean onGround) {
        super(construct(onGround));
    }

    private static Object construct(boolean onGround) {
        try {
            Constructor<?> ctor = (Constructor<?>) Mappings.C03_PACKET_PLAYER_CONSTRUCTOR;
            return ReflectionUtils.invokeConstructor(ctor, onGround);
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct C04PacketPlayerPosition", e);
        }
    }

    public void setRotation(float yaw, float pitch) {
        try {
            ReflectionUtils.setFieldValue(handle, (Field) Mappings.C03_PACKET_PLAYER_YAW, yaw);
            ReflectionUtils.setFieldValue(handle, (Field) Mappings.C03_PACKET_PLAYER_PITCH, pitch);
            ReflectionUtils.setFieldValue(handle, (Field) Mappings.C03_PACKET_PLAYER_ROTATING, true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set packet rotation", e);
        }
    }

    public boolean isRotating() {
        try {
            return ReflectionUtils.getBoolean((Field) Mappings.C03_PACKET_PLAYER_ROTATING, handle);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read packet rotation flag", e);
        }
    }

    public float getYaw() {
        try {
            return ReflectionUtils.getFloat((Field) Mappings.C03_PACKET_PLAYER_YAW, handle);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read packet yaw", e);
        }
    }

    public float getPitch() {
        try {
            return ReflectionUtils.getFloat((Field) Mappings.C03_PACKET_PLAYER_PITCH, handle);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read packet pitch", e);
        }
    }
}
