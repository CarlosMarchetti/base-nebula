package dev.nebula.client.utils;

public final class AimGeometry {
    private AimGeometry() {}

    public static float[] correctInput(float strafe, float forward, float cameraYaw, float movementYaw) {
        if (strafe == 0f && forward == 0f) return new float[] {0f, 0f};
        double delta = Math.toRadians(cameraYaw - movementYaw);
        double desiredStrafe = strafe * Math.cos(delta) - forward * Math.sin(delta);
        double desiredForward = strafe * Math.sin(delta) + forward * Math.cos(delta);
        double best = -Double.MAX_VALUE;
        int bestS = 0, bestF = 0;
        for (int s = -1; s <= 1; s++) for (int f = -1; f <= 1; f++) {
            if (s == 0 && f == 0) continue;
            double alignment = (desiredStrafe * s + desiredForward * f) / Math.sqrt(s * s + f * f);
            if (alignment > best) { best = alignment; bestS = s; bestF = f; }
        }
        float magnitude = Math.max(Math.abs(strafe), Math.abs(forward));
        return new float[] {bestS * magnitude, bestF * magnitude};
    }

    public static boolean intersects(double x, double y, double z, float yaw, float pitch, double range,
            double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (!Double.isFinite(range) || range < 0 || !Float.isFinite(yaw) || !Float.isFinite(pitch)) return false;
        double yawRad = Math.toRadians(yaw), pitchRad = Math.toRadians(pitch);
        double[] origin = {x, y, z};
        double[] direction = {-Math.sin(yawRad) * Math.cos(pitchRad), -Math.sin(pitchRad), Math.cos(yawRad) * Math.cos(pitchRad)};
        double[] min = {minX, minY, minZ}, max = {maxX, maxY, maxZ};
        double near = 0, far = range;
        for (int i = 0; i < 3; i++) {
            if (!Double.isFinite(origin[i]) || !Double.isFinite(min[i]) || !Double.isFinite(max[i]) || min[i] > max[i]) return false;
            if (Math.abs(direction[i]) < 1e-10) {
                if (origin[i] < min[i] || origin[i] > max[i]) return false;
                continue;
            }
            double a = (min[i] - origin[i]) / direction[i], b = (max[i] - origin[i]) / direction[i];
            near = Math.max(near, Math.min(a, b));
            far = Math.min(far, Math.max(a, b));
            if (far < near) return false;
        }
        return true;
    }
}
