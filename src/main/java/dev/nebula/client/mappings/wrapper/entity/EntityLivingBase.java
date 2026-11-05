package dev.nebula.client.mappings.wrapper.entity;

import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class EntityLivingBase extends Entity {

	public EntityLivingBase(Object handle) {
	    super(handle);
	}

	public static boolean isInstance(Object obj) {
	    if (obj == null) return false;
	    
	    Object realHandle = obj;
	    if (obj instanceof Entity) {
	        realHandle = ((Entity) obj).handle;
	    }
	    
        if (Mappings.ENTITY_LIVING_BASE_CLASS != null) {
            return Mappings.ENTITY_LIVING_BASE_CLASS.isInstance(realHandle);
        }
	    return false;
	}

    public float getHealth() {
        try {
            if (Mappings.ENTITY_GET_HEALTH != null) {
                Object res = ReflectionUtils.invokeMethod((Method) Mappings.ENTITY_GET_HEALTH, handle);
                if (res instanceof Number) {
                    return ((Number) res).floatValue();
                }
            }
        } catch (Exception ignored) {}

        try {
            Method m = handle.getClass().getMethod("getHealth");
            Object res = m.invoke(handle);
            if (res instanceof Number) {
                return ((Number) res).floatValue();
            }
        } catch (Exception ignored) {}

        throw new IllegalStateException("Entity health unavailable");
    }
    
    public int getHurtResistantTime() {
        try {
            if (Mappings.ENTITY_LIVING_HURT_RESISTANT_TIME != null) {
                return ReflectionUtils.getInt((Field) Mappings.ENTITY_LIVING_HURT_RESISTANT_TIME, handle);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public float getMoveForward() {
        try {
            if (Mappings.MOVE_FORWARD_FIELD != null) {
                return (float) ReflectionUtils.getFloat((Field) Mappings.MOVE_FORWARD_FIELD, handle);
            }
        } catch (Exception ignored) {}
        return 0.0f;
    }

    public float getMoveStrafing() {
        try {
            if (Mappings.MOVE_STRAFING_FIELD != null) {
                return (float) ReflectionUtils.getFloat((Field) Mappings.MOVE_STRAFING_FIELD, handle);
            }
        } catch (Exception ignored) {}
        return 0.0f;
    }
}
