package dev.nebula.client.features;

public final class Projection {
    private Projection() {}

    public static float[] point(
            double x, double y, double z, float[] model, float[] projection, int[] viewport) {
        double[] v = {x, y, z, 1}, a = new double[4], b = new double[4];
        for (int row = 0; row < 4; row++)
            for (int column = 0; column < 4; column++)
                a[row] += model[column * 4 + row] * v[column];
        for (int row = 0; row < 4; row++)
            for (int column = 0; column < 4; column++)
                b[row] += projection[column * 4 + row] * a[column];
        if (b[3] <= .0001) return null;
        double depth = b[2] / b[3];
        if (depth < -1 || depth > 1) return null;
        double sx = viewport[0] + (b[0] / b[3] + 1) * viewport[2] / 2,
                sy = viewport[3] - (b[1] / b[3] + 1) * viewport[3] / 2;
        return Double.isFinite(sx) && Double.isFinite(sy)
                ? new float[] {(float) sx, (float) sy}
                : null;
    }
}
