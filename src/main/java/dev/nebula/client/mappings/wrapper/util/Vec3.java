package dev.nebula.client.mappings.wrapper.util;

import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.utils.ReflectionUtils;

public class Vec3 {

    public final Object handle;

    public Vec3(Object handle) {
        this.handle = handle;
    }

    public Vec3(double x, double y, double z) {
        try {
            this.handle = ReflectionUtils.invokeConstructor(
                    Mappings.VEC3_CONSTRUCTOR,
                    x, y, z
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct Vec3(x,y,z)", e);
        }
    }

    public double getX() {
        try {
            return ReflectionUtils.getDouble(Mappings.VEC3_X, handle);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Vec3 x", e);
        }
    }

    public double getY() {
        try {
            return ReflectionUtils.getDouble(Mappings.VEC3_Y, handle);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Vec3 y", e);
        }
    }

    public double getZ() {
        try {
            return ReflectionUtils.getDouble(Mappings.VEC3_Z, handle);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Vec3 z", e);
        }
    }

    public Vec3 normalize() {
        try {
            Object normalized = ReflectionUtils.invokeMethod(Mappings.VEC3_NORMALIZE, handle);
            return new Vec3(normalized);
        } catch (Exception e) {
            throw new RuntimeException("Failed to normalize Vec3", e);
        }
    }

    public Vec3 add(double x, double y, double z) {
        try {
            Object result = ReflectionUtils.invokeMethod(
                    Mappings.VEC3_ADD_VECTOR,
                    handle,
                    x, y, z
            );
            return new Vec3(result);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add vector", e);
        }
    }

    public Vec3 add(Vec3 other) {
        return add(other.getX(), other.getY(), other.getZ());
    }

    public double distance(Vec3 other) {
        double dx = getX() - other.getX();
        double dy = getY() - other.getY();
        double dz = getZ() - other.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public double dotProduct(Vec3 other) {
        return getX() * other.getX() + getY() * other.getY() + getZ() * other.getZ();
    }

    public Object getHandle() {
        return handle;
    }

    @Override
    public String toString() {
        return String.format("Vec3(%.2f, %.2f, %.2f)", getX(), getY(), getZ());
    }
}
