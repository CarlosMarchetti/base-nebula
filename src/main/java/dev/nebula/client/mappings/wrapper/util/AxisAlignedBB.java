package dev.nebula.client.mappings.wrapper.util;

import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.utils.ReflectionUtils;

import java.lang.reflect.Field;

public class AxisAlignedBB {

    private final Object handle;

    public AxisAlignedBB(Object handle) {
        this.handle = handle;
    }

    public Object getHandle() {
        return handle;
    }

    public double getMinX() {
        try {
            return ReflectionUtils.getDouble((Field) Mappings.AXIS_ALIGNED_BB_MIN_X, handle);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get minX", e);
        }
    }

    public double getMinY() {
        try {
            return ReflectionUtils.getDouble((Field) Mappings.AXIS_ALIGNED_BB_MIN_Y, handle);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get minY", e);
        }
    }

    public double getMinZ() {
        try {
            return ReflectionUtils.getDouble((Field) Mappings.AXIS_ALIGNED_BB_MIN_Z, handle);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get minZ", e);
        }
    }

    public double getMaxX() {
        try {
            return ReflectionUtils.getDouble((Field) Mappings.AXIS_ALIGNED_BB_MAX_X, handle);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get maxX", e);
        }
    }

    public double getMaxY() {
        try {
            return ReflectionUtils.getDouble((Field) Mappings.AXIS_ALIGNED_BB_MAX_Y, handle);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get maxY", e);
        }
    }

    public double getMaxZ() {
        try {
            return ReflectionUtils.getDouble((Field) Mappings.AXIS_ALIGNED_BB_MAX_Z, handle);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get maxZ", e);
        }
    }
}
