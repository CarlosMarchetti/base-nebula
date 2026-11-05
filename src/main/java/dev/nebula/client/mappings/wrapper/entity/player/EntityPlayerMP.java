package dev.nebula.client.mappings.wrapper.entity.player;

import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.mappings.wrapper.entity.Entity;
import dev.nebula.client.utils.ReflectionUtils;

import java.lang.reflect.Field;

public class EntityPlayerMP extends EntityPlayer {

    public EntityPlayerMP(Object handle) {
        super(handle);
    }
    
    public static boolean isInstance(Object obj) {
        if (obj == null) return false;

        Object realHandle = obj;
        if (obj instanceof Entity) {
            realHandle = ((Entity) obj).handle;
        }

        return Mappings.ENTITY_PLAYER_MP_CLASS.isInstance(realHandle);
    }


    public int getPing() {
        try {
            return ReflectionUtils.getInt((Field) Mappings.PING_FIELD, handle);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get ping from EntityPlayerMP", e);
        }
    }
}
