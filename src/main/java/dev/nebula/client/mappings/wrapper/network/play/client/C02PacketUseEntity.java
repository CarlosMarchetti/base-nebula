package dev.nebula.client.mappings.wrapper.network.play.client;

import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.mappings.wrapper.entity.Entity;
import dev.nebula.client.mappings.wrapper.network.Packet;
import dev.nebula.client.utils.Log;
import dev.nebula.client.utils.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public class C02PacketUseEntity extends Packet {

    public C02PacketUseEntity(Entity entity, Action action) {
        super(construct(entity, action));
    }

    private static Object construct(Entity entity, Action action) {
        Object entityHandle = (entity != null) ? entity.getHandle() : null;
        if (entityHandle == null || action == null) {
            throw new IllegalArgumentException("Attack requires an entity and action");
        }

        try {
            if (Mappings.C02_PACKET_USE_ENTITY_CONSTRUCTOR_ENTITY_ACTION != null && Mappings.C02_PACKET_USE_ENTITY_ACTION_ATTACK != null) {
                Constructor<?> ctor = (Constructor<?>) Mappings.C02_PACKET_USE_ENTITY_CONSTRUCTOR_ENTITY_ACTION;
                Object actionEnum = getActionEnum(action);
                Object packet = ReflectionUtils.invokeConstructor(ctor, entityHandle, actionEnum);
                if (packet != null) return packet;
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> packetClass = ReflectionUtils.getClass("net.minecraft.network.play.client.C02PacketUseEntity");
            Class<?> actionEnumClass = ReflectionUtils.getClass("net.minecraft.network.play.client.C02PacketUseEntity$Action");
            Class<?> entityClass = ReflectionUtils.getClass("net.minecraft.entity.Entity");

            Object attackEnum = null;
            for (Object constant : actionEnumClass.getEnumConstants()) {
                if (constant.toString().equalsIgnoreCase("ATTACK")) {
                    attackEnum = constant;
                    break;
                }
            }

            if (attackEnum == null) throw new IllegalStateException("ATTACK enum missing");
            for (Constructor<?> ctor : packetClass.getConstructors()) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length == 2 && params[0].isAssignableFrom(entityClass) && params[1].equals(actionEnumClass)) {
                    ctor.setAccessible(true);
                    return ctor.newInstance(entityHandle, attackEnum);
                }
            }
        } catch (Throwable t) {
            Log.error("Erro ao construir C02PacketUseEntity fallback: " + t.getMessage());
        }

        throw new IllegalStateException("C02 ATTACK unavailable: check packet constructor and Action mappings");
    }

    public enum Action {
        ATTACK
    }

    private static Object getActionEnum(Action action) throws Exception {
        switch (action) {
            case ATTACK:
                Field f = Mappings.C02_PACKET_USE_ENTITY_ACTION_ATTACK;
                if (f != null) {
                    return f.get(null);
                }
                Class<?> actionEnumClass = ReflectionUtils.getClass("net.minecraft.network.play.client.C02PacketUseEntity$Action");
                for (Object constant : actionEnumClass.getEnumConstants()) {
                    if (constant.toString().equalsIgnoreCase("ATTACK")) {
                        return constant;
                    }
                }
                return null;

            default:
                throw new IllegalArgumentException("Unknown action: " + action);
        }
    }
}
