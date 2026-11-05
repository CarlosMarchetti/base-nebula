package dev.nebula.client.utils.math;

public class Vec3 {

    private final dev.nebula.client.mappings.wrapper.util.Vec3 mcVec;
    public Vec3(double x, double y, double z) {
        this.mcVec = new dev.nebula.client.mappings.wrapper.util.Vec3(x, y, z);
    }

    public Vec3(dev.nebula.client.mappings.wrapper.util.Vec3 mcVec) {
        if (mcVec == null) throw new IllegalArgumentException("mcVec cannot be null");
        this.mcVec = mcVec;
    }
    
    public static Vec3 fromEntity(dev.nebula.client.mappings.wrapper.entity.Entity entity) {
        return new Vec3(entity.getPosX(), entity.getPosY(), entity.getPosZ());
    }

    public static Vec3 fromEntityEyes(dev.nebula.client.mappings.wrapper.entity.EntityLivingBase entity) {
        return new Vec3(entity.getPosX(), entity.getPosY() + entity.getActualEyeHeight(), entity.getPosZ());
    }

    public double getX() { return mcVec.getX(); }
    public double getY() { return mcVec.getY(); }
    public double getZ() { return mcVec.getZ(); }

    public Vec3 add(double x, double y, double z) {
        return new Vec3(mcVec.add(x, y, z));
    }

    public Vec3 add(Vec3 other) {
        return add(other.getX(), other.getY(), other.getZ());
    }

    public double distance(Vec3 other) {
        return mcVec.distance(other.mcVec);
    }

    public Vec3 normalize() {
        return new Vec3(mcVec.normalize());
    }

    public double dotProduct(Vec3 other) {
        return mcVec.dotProduct(other.mcVec);
    }

    public dev.nebula.client.mappings.wrapper.util.Vec3 toMCVec3() {
        return mcVec;
    }

    public Object getHandle() {
        return mcVec.getHandle();
    }

    @Override
    public String toString() {
        return String.format("Vec3(%.2f, %.2f, %.2f)", getX(), getY(), getZ());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vec3)) return false;
        Vec3 vec3 = (Vec3) o;
        return Double.compare(vec3.getX(), getX()) == 0 &&
               Double.compare(vec3.getY(), getY()) == 0 &&
               Double.compare(vec3.getZ(), getZ()) == 0;
    }

    @Override
    public int hashCode() {
        long temp = Double.doubleToLongBits(getX());
        int result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(getY());
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(getZ());
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
