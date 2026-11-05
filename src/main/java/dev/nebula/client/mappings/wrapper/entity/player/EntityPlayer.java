package dev.nebula.client.mappings.wrapper.entity.player;

import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.mappings.wrapper.entity.Entity;
import dev.nebula.client.mappings.wrapper.entity.EntityLivingBase;
import dev.nebula.client.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class EntityPlayer extends EntityLivingBase {

    public EntityPlayer(Object handle) {
        super(handle);
    }

    public static boolean isInstance(Object obj) {
        if (obj == null) return false;

        Object realHandle = obj;
        if (obj instanceof Entity) {
            realHandle = ((Entity) obj).handle;
        }

        return Mappings.ENTITY_PLAYER_CLASS.isInstance(realHandle);
    }

    @Override
    public String getName() {
        try {
            Object chatComponent = ReflectionUtils.invokeMethod((Method) Mappings.ENTITY_PLAYER_GET_NAME, handle);
            return (String) ReflectionUtils.invokeMethod((Method) Mappings.I_CHAT_COMPONENT_GET_FORMATTED_TEXT, chatComponent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get player name", e);
        }
    }

    public Object getInventory() {
        try {
            return ReflectionUtils.getFieldValue((Field) Mappings.ENTITY_PLAYER_INVENTORY, handle);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get inventory", e);
        }
    }
}
