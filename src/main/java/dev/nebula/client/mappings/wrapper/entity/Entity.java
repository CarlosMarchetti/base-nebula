package dev.nebula.client.mappings.wrapper.entity;

import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.mappings.wrapper.entity.player.EntityPlayer;
import dev.nebula.client.mappings.wrapper.util.AxisAlignedBB;
import dev.nebula.client.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public class Entity {

    public final Object handle;

    public Entity(Object handle) {
        this.handle = handle;
    }
    
    public static boolean isInstance(Object obj) {
        if (obj == null) return false;

        Object realHandle = obj;
        if (obj instanceof Entity) {
            realHandle = ((Entity) obj).handle;
        }

        if (Mappings.ENTITY_CLASS != null) {
            return Mappings.ENTITY_CLASS.isInstance(realHandle);
        }
        return false;
    }

    public Object getHandle() {
        return handle;
    }
    
    public double getPosX() {
        if(Mappings.ENTITY_POS_X==null)throw new IllegalStateException("ENTITY_POS_X mapping unavailable");
        try{return ReflectionUtils.getDouble(Mappings.ENTITY_POS_X,handle);}
        catch(Exception e){throw new IllegalStateException("Cannot read entity position X",e);}
    }

    public double getPosY() {
        if(Mappings.ENTITY_POS_Y==null)throw new IllegalStateException("ENTITY_POS_Y mapping unavailable");
        try{return ReflectionUtils.getDouble(Mappings.ENTITY_POS_Y,handle);}
        catch(Exception e){throw new IllegalStateException("Cannot read entity position Y",e);}
    }

    public double getPosZ() {
        if(Mappings.ENTITY_POS_Z==null)throw new IllegalStateException("ENTITY_POS_Z mapping unavailable");
        try{return ReflectionUtils.getDouble(Mappings.ENTITY_POS_Z,handle);}
        catch(Exception e){throw new IllegalStateException("Cannot read entity position Z",e);}
    }

    public double getMotionX() {
        try {
            if (Mappings.ENTITY_MOTION_X != null) {
                return ReflectionUtils.getDouble((Field) Mappings.ENTITY_MOTION_X, handle);
            }
        } catch (Exception ignored) {}
        return 0.0;
    }

    public double getMotionY() {
        try {
            if (Mappings.ENTITY_MOTION_Y != null) {
                return ReflectionUtils.getDouble((Field) Mappings.ENTITY_MOTION_Y, handle);
            }
        } catch (Exception ignored) {}
        return 0.0;
    }

    public double getMotionZ() {
        try {
            if (Mappings.ENTITY_MOTION_Z != null) {
                return ReflectionUtils.getDouble((Field) Mappings.ENTITY_MOTION_Z, handle);
            }
        } catch (Exception ignored) {}
        return 0.0;
    }

    public float getEntityHeight() {
        try {
            if (Mappings.ENTITY_HEIGHT_FIELD != null) {
                return (float) ReflectionUtils.getFieldValue(Mappings.ENTITY_HEIGHT_FIELD, handle);
            }
        } catch (Exception ignored) {}
        return 1.8f;
    }

    public float getRotationYaw() {
        try {
            if (Mappings.ENTITY_ROTATION_YAW != null) {
                return ReflectionUtils.getFloat((Field) Mappings.ENTITY_ROTATION_YAW, handle);
            }
        } catch (Exception ignored) {}
        return 0.0f;
    }

    public float getRotationPitch() {
        try {
            if (Mappings.ENTITY_ROTATION_PITCH != null) {
                return ReflectionUtils.getFloat((Field) Mappings.ENTITY_ROTATION_PITCH, handle);
            }
        } catch (Exception ignored) {}
        return 0.0f;
    }

    public boolean isOnGround() {
        try {
            if (Mappings.ENTITY_ON_GROUND != null) {
                return ReflectionUtils.getBoolean((Field) Mappings.ENTITY_ON_GROUND, handle);
            }
        } catch (Exception ignored) {}
        return true;
    }

    public boolean isInvisible() {
        try {
            if (Mappings.ENTITY_IS_INVISIBLE != null) {
                return (boolean) ReflectionUtils.invokeMethod(
                        (Method) Mappings.ENTITY_IS_INVISIBLE,
                        handle
                );
            }
        } catch (Exception ignored) {}
        return false;
    }

    public boolean isSneaking() {
        try {
            if (Mappings.ENTITY_IS_SNEAKING != null) {
                return (boolean) ReflectionUtils.invokeMethod((Method) Mappings.ENTITY_IS_SNEAKING, handle);
            }
        } catch (Exception ignored) {}
        return false;
    }

    public boolean isInWater() {
        try {
            if (Mappings.ENTITY_IS_IN_WATER != null) {
                return (boolean) ReflectionUtils.invokeMethod((Method) Mappings.ENTITY_IS_IN_WATER, handle);
            }
        } catch (Exception ignored) {}
        return false;
    }

    public boolean isInWeb() {
        try {
            if (Mappings.ISINWEB != null) {
                return ReflectionUtils.getBoolean((Field) Mappings.ISINWEB, handle);
            }
        } catch (Exception ignored) {}
        return false;
    }

    public int getEntityId() {
        try {
            if (Mappings.ENTITY_ID_FIELD != null) {
                return ReflectionUtils.getInt((Field) Mappings.ENTITY_ID_FIELD, handle);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public UUID getUniqueID() {
        try {
            if (Mappings.ENTITY_GET_UNIQUE_ID != null) {
                return (UUID) ReflectionUtils.invokeMethod((Method) Mappings.ENTITY_GET_UNIQUE_ID, handle);
            }
        } catch (Exception ignored) {}
        return UUID.randomUUID();
    }

    public String getName() {
        try {
            if (Mappings.ENTITY_PLAYER_GET_NAME != null) {
                Object name = ReflectionUtils.invokeMethod(
                        (Method) Mappings.ENTITY_PLAYER_GET_NAME,
                        handle
                );
                return (name != null ? name.toString() : "");
            }
        } catch (Exception ignored) {}
        return "";
    }

    public AxisAlignedBB getBoundingBox() {
        try {
            if (Mappings.ENTITY_GET_BOUNDING_BOX != null) {
                Object raw = ReflectionUtils.invokeMethod(
                        (Method) Mappings.ENTITY_GET_BOUNDING_BOX,
                        handle
                );
                return (raw != null) ? new AxisAlignedBB(raw) : null;
            }
        } catch (Exception ignored) {}
        return null;
    }
    
    public Object getRawBoundingBox() {
        try {
            if (Mappings.ENTITY_GET_BOUNDING_BOX != null) {
                return ReflectionUtils.invokeMethod(
                        (Method) Mappings.ENTITY_GET_BOUNDING_BOX,
                        handle
                );
            }
        } catch (Exception ignored) {}
        return null;
    }

    public double getDistanceToEntity(Entity entity) {
        if (entity == null) return Double.MAX_VALUE;
        double dx = getPosX() - entity.getPosX();
        double dy = getPosY() - entity.getPosY();
        double dz = getPosZ() - entity.getPosZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public Object getPositionVec() {
        try {
            if (Mappings.ENTITY_GET_POSITION != null) {
                return ReflectionUtils.invokeMethod((Method) Mappings.ENTITY_GET_POSITION, handle);
            }
        } catch (Exception ignored) {}
        return null;
    }

    public float getActualEyeHeight(){
        if(Mappings.ENTITY_GET_EYE_HEIGHT==null)throw new IllegalStateException("ENTITY_GET_EYE_HEIGHT mapping unavailable");
        try{return ((Number)ReflectionUtils.invokeMethod(Mappings.ENTITY_GET_EYE_HEIGHT,handle)).floatValue();}
        catch(Exception e){throw new IllegalStateException("Cannot read eye height",e);}
    }
    @Deprecated
    public static float getEyeHeight() {
        return 1.62F;
    }

	public boolean isDead() {
		try {
            if (Mappings.ENTITY_IS_DEAD != null) {
                return ReflectionUtils.getBoolean((Field) Mappings.ENTITY_IS_DEAD, handle);
            }
        } catch (Exception ignored) {}

        try {
            Field f = handle.getClass().getField("isDead");
            return f.getBoolean(handle);
        } catch (Exception ignored) {}

        return false;
	}

	public void setMotionX(double motionX) {
	    try {
            if (Mappings.ENTITY_MOTION_X != null) {
	            ReflectionUtils.setFieldValue(handle, (Field) Mappings.ENTITY_MOTION_X, motionX);
            }
	    } catch (Exception ignored) {}
	}

	public void setMotionY(double motionY) {
	    try {
            if (Mappings.ENTITY_MOTION_Y != null) {
	            ReflectionUtils.setFieldValue(handle, (Field) Mappings.ENTITY_MOTION_Y, motionY);
            }
	    } catch (Exception ignored) {}
	}

	public void setMotionZ(double motionZ) {
	    try {
            if (Mappings.ENTITY_MOTION_Z != null) {
	            ReflectionUtils.setFieldValue(handle, (Field) Mappings.ENTITY_MOTION_Z, motionZ);
            }
	    } catch (Exception ignored) {}
	}
}
