package dev.nebula.client.utils;

import dev.nebula.client.mappings.wrapper.client.Minecraft;
import dev.nebula.client.mappings.wrapper.client.entity.EntityPlayerSP;
import dev.nebula.client.mappings.wrapper.entity.Entity;
import dev.nebula.client.mappings.wrapper.util.Vec3;
import dev.nebula.client.utils.math.MathHelper;

public final class RotationUtils {

    private static Rotation currentRotation = new Rotation(0f, 0f);
    private static Rotation targetRotation = null;
    private static Object sentPlayer, sentWorld;
    private static boolean returning, finishReturn;
    private static float returnStep = 12;

    public static Rotation stepToward(Rotation from, Rotation to, float step) {
        float limit = Math.max(.1f, step);
        return new Rotation(
                from.getYaw()
                        + MathHelper.clamp_float(
                                MathHelper.wrapAngleTo180_float(to.getYaw() - from.getYaw()),
                                -limit,
                                limit),
                from.getPitch()
                        + MathHelper.clamp_float(to.getPitch() - from.getPitch(), -limit, limit));
    }

    public static Rotation serverOrCamera(EntityPlayerSP player) {
        Object world = Minecraft.getWorld() == null ? null : Minecraft.getWorld().getHandle();
        return sentPlayer == player.getHandle() && sentWorld == world
                ? currentRotation
                : new Rotation(player.getRotationYaw(), player.getRotationPitch());
    }

    public static Rotation cameraBaseline(EntityPlayerSP player) {
        Rotation previous = serverOrCamera(player);
        return new Rotation(
                previous.getYaw()
                        + MathHelper.wrapAngleTo180_float(
                                player.getRotationYaw() - previous.getYaw()),
                player.getRotationPitch());
    }

    public static void recordServerRotation(Object player, Rotation rotation) {
        currentRotation = rotation;
        sentPlayer = player;
        sentWorld = Minecraft.getWorld() == null ? null : Minecraft.getWorld().getHandle();
        if (finishReturn) {
            targetRotation = null;
            returning = finishReturn = false;
        }
    }

    public static void releaseTargetRotation(float step) {
        EntityPlayerSP player = Minecraft.getPlayer();
        if (player == null) {
            clearTargetRotation();
            return;
        }
        targetRotation = serverOrCamera(player);
        returnStep = Math.max(1, Math.min(45, step));
        returning = true;
        finishReturn = false;
    }

    public static void advanceRelease() {
        if (!returning) return;
        EntityPlayerSP player = Minecraft.getPlayer();
        Object world = Minecraft.getWorld() == null ? null : Minecraft.getWorld().getHandle();
        if (player == null || player.getHandle() != sentPlayer || world != sentWorld) {
            clearTargetRotation();
            return;
        }
        Rotation camera = new Rotation(player.getRotationYaw(), player.getRotationPitch());
        targetRotation = stepToward(currentRotation, camera, returnStep);
        finishReturn = targetRotation.getAngleDifference(camera) < .001f;
    }

    public static boolean isReturning() {
        return returning;
    }

    public static void resetSession() {
        clearTargetRotation();
        sentPlayer = sentWorld = null;
    }


    public static void setTargetRotation(Rotation rotation) {
        if (rotation != null) {
            returning = finishReturn = false;
            targetRotation = rotation;
        }
    }

    public static void setTargetRotation(float yaw, float pitch) {
        setTargetRotation(new Rotation(yaw, pitch));
    }

    public static void clearTargetRotation() {
        targetRotation = null;
        returning = finishReturn = false;
    }

    public static float movementYaw(Object entity, float cameraYaw) {
        Rotation rotation = targetRotation;
        EntityPlayerSP player = Minecraft.getPlayer();
        return rotation != null && player != null && player.getHandle() == entity
                ? rotation.getYaw()
                : cameraYaw;
    }

    public static Rotation getCurrentRotation() {
        return currentRotation;
    }

    public static Rotation getTargetRotation() {
        return targetRotation;
    }

    public static void resetRotation() {
        EntityPlayerSP player = Minecraft.getPlayer();
        if (player != null) {
            currentRotation = new Rotation(player.getRotationYaw(), player.getRotationPitch());
        }
        clearTargetRotation();
    }


    public static Rotation getRotationToEntity(Entity entity) {
        EntityPlayerSP player = Minecraft.getPlayer();
        if (player == null || entity == null) return currentRotation;

        Vec3 eyes = getEyePos(player);

        double targetY = entity.getPosY() + entity.getActualEyeHeight() * 0.85;

        Vec3 target = new Vec3(entity.getPosX(), targetY, entity.getPosZ());

        return getRotationFromVec(eyes, target);
    }

    public static Rotation getRotationFromVec(Vec3 from, Vec3 to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();

        double distance = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, distance));

        return new Rotation(
                MathHelper.wrapAngleTo180_float(yaw), MathHelper.clamp_float(pitch, -90f, 90f));
    }

    public static Rotation applySensitivityPatch(
            Rotation targetRotation, Rotation previousRotation) {
        float mouseSensitivity = 0.5f;
        try {
            Object settings =
                    dev.nebula.client.utils.ReflectionUtils.getFieldValue(
                            dev.nebula.client.mappings.Mappings.MINECRAFT_GAME_SETTINGS,
                            Minecraft.getMinecraft().getRaw());
            java.lang.reflect.Field field =
                    ReflectionUtils.getField(settings.getClass(), "mouseSensitivity");
            mouseSensitivity = ((Number) field.get(settings)).floatValue();
        } catch (Exception ignored) {
        }
        float f = mouseSensitivity * 0.6F + 0.2F;
        float f1 = f * f * f * 1.2F;

        float deltaYaw =
                MathHelper.wrapAngleTo180_float(
                        targetRotation.getYaw() - previousRotation.getYaw());
        float deltaPitch = targetRotation.getPitch() - previousRotation.getPitch();

        deltaYaw -= deltaYaw % f1;
        deltaPitch -= deltaPitch % f1;

        return new Rotation(
                previousRotation.getYaw() + deltaYaw, previousRotation.getPitch() + deltaPitch);
    }


    public static Vec3 getEyePos(EntityPlayerSP player) {
        return new Vec3(
                player.getPosX(), player.getPosY() + player.getActualEyeHeight(), player.getPosZ());
    }

    public static boolean isInFOV(Entity entity, float fov) {
        EntityPlayerSP player = Minecraft.getPlayer();
        if (player == null || entity == null) return false;

        Rotation needed = getRotationToEntity(entity);
        float yawDiff =
                Math.abs(
                        MathHelper.wrapAngleTo180_float(needed.getYaw() - player.getRotationYaw()));

        return yawDiff <= fov / 2f;
    }


    public static class Rotation {
        private final float yaw;
        private final float pitch;

        public Rotation(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = MathHelper.clamp_float(pitch, -90f, 90f);
        }

        public float getYaw() {
            return yaw;
        }

        public float getPitch() {
            return pitch;
        }

        public void applyToPlayer() {
            EntityPlayerSP player = Minecraft.getPlayer();
            if (player != null) {
                player.setRotationYaw(yaw);
                player.setRotationPitch(pitch);
            }
        }

        public float getAngleDifference(Rotation other) {
            float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(this.yaw - other.yaw));
            float pitchDiff = Math.abs(this.pitch - other.pitch);
            return (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
        }

        @Override
        public String toString() {
            return String.format("Rotation[yaw=%.2f, pitch=%.2f]", yaw, pitch);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Rotation)) return false;
            Rotation other = (Rotation) obj;
            return Math.abs(this.yaw - other.yaw) < 0.01f
                    && Math.abs(this.pitch - other.pitch) < 0.01f;
        }
    }
}
