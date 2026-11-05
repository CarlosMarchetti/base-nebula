package dev.nebula.client.mappings.wrapper.client.entity;

import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.mappings.wrapper.entity.Entity;
import dev.nebula.client.mappings.wrapper.entity.player.EntityPlayer;
import dev.nebula.client.mappings.wrapper.item.ItemStack;
import dev.nebula.client.mappings.wrapper.network.play.client.C0APacketAnimation;
import dev.nebula.client.mappings.wrapper.util.MovementInput;
import dev.nebula.client.utils.PacketUtils;
import dev.nebula.client.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class EntityPlayerSP extends EntityPlayer {
    
    public EntityPlayerSP(Object handle) {
        super(handle);
    }
    
    public void swingItem() {
        boolean swung = false;
        try {
            if (Mappings.ENTITY_PLAYER_SWING_ITEM != null) {
                ReflectionUtils.invokeMethod(Mappings.ENTITY_PLAYER_SWING_ITEM, handle);
                swung = true;
            }
        } catch (Exception ignored) {}

        if (!swung && handle != null) {
            try {
                Method m = handle.getClass().getMethod("swingItem");
                m.setAccessible(true);
                m.invoke(handle);
                swung = true;
            } catch (Exception ignored) {}
        }

        if (!swung) {
            try {
                PacketUtils.sendPacket(new C0APacketAnimation());
            } catch (Exception ignored) {}
        }
    }
    
    public boolean isOnGround() {
        try {
            return ReflectionUtils.getBoolean(Mappings.ENTITY_ON_GROUND, handle);
        } catch (Exception ignored) {
            return true;
        }
    }
    
    public Object getMovementInput() {
        try {
            return ReflectionUtils.getFieldValue((Field) Mappings.ENTITY_PLAYER_MOVEMENT_INPUT, handle);
        } catch (Exception ignored) {
            return null;
        }
    }
    
    public MovementInput getMovementInputWrapper() {
        Object input = getMovementInput(); 
        if (input == null) return null;
        return new MovementInput(input);
    }
    
    public boolean isInWeb() {
        try {
            return ReflectionUtils.getBoolean(Mappings.ISINWEB, handle);
        } catch (Exception ignored) {
            return false;
        }
    }
    
    public double getDistanceToEntity(Entity entity) {
        if (entity == null) return Double.MAX_VALUE;
        double dx = this.getPosX() - entity.getPosX();
        double dy = this.getPosY() - entity.getPosY();
        double dz = this.getPosZ() - entity.getPosZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    public boolean canEntityBeSeen(Entity entity) {
        try {
            Object result = ReflectionUtils.invokeMethod(Mappings.ENTITY_CAN_BE_COLLIDED_WITH, entity.getHandle());
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            return true;
        }
    }
    
    public void setRotationPitch(float pitch) {
        try {
            ReflectionUtils.setFieldValue(handle, (Field) Mappings.ENTITY_ROTATION_PITCH, pitch);
        } catch (Exception ignored) {}
    }
    
    public void setRotationYaw(float yaw) {
        try {
            ReflectionUtils.setFieldValue(handle, (Field) Mappings.ENTITY_ROTATION_YAW, yaw);
        } catch (Exception ignored) {}
    }
    
    public void setRotations(float yaw, float pitch) {
        setRotationYaw(yaw);
        setRotationPitch(pitch);
    }
    
    public boolean isSprinting() {
        if(Mappings.ENTITY_IS_SPRINTING==null)throw new IllegalStateException("ENTITY_IS_SPRINTING mapping unavailable");
        try{return (Boolean)ReflectionUtils.invokeMethod(Mappings.ENTITY_IS_SPRINTING,handle);}
        catch(Exception e){throw new IllegalStateException("Cannot read sprint state",e);}
    }
    
    public void setSprinting(boolean sprinting) {
        try {
            ReflectionUtils.invokeMethod(Mappings.ENTITY_PLAYER_SP_SET_SPRINTING, handle, sprinting);
        } catch (Exception ignored) {}
    }
    
    public ItemStack getHeldItem() {
        try {
            Object rawStack = ReflectionUtils.invokeMethod(
                    Mappings.ENTITY_PLAYER_GET_CURRENT_ITEM,
                    handle
            );
            if (rawStack == null) return null;
            return new ItemStack(rawStack);
        } catch (Exception e) {
            return null;
        }
    }
}
