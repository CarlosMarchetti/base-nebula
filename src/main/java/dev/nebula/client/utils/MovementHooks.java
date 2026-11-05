package dev.nebula.client.utils;
import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.mappings.wrapper.client.entity.EntityPlayerSP;
import dev.nebula.client.utils.RotationUtils.Rotation;

public final class MovementHooks {
    private static boolean logged;
    private MovementHooks() {}
    public static void updateInput(Object raw) {
        Rotation rotation = RotationUtils.getTargetRotation();
        if (rotation == null) return;
        try {
            Object input = ReflectionUtils.getFieldValue(Mappings.ENTITY_PLAYER_MOVEMENT_INPUT, raw);
            if (input == null || Mappings.MOVEMENT_INPUT_FORWARD == null || Mappings.MOVEMENT_INPUT_STRAFE == null)
                throw new IllegalStateException("Movement input mappings unavailable");
            float forward = ReflectionUtils.getFloat(Mappings.MOVEMENT_INPUT_FORWARD, input);
            float strafe = ReflectionUtils.getFloat(Mappings.MOVEMENT_INPUT_STRAFE, input);
            float[] corrected = AimGeometry.correctInput(strafe, forward, new EntityPlayerSP(raw).getRotationYaw(), rotation.getYaw());
            ReflectionUtils.setFloat(Mappings.MOVEMENT_INPUT_STRAFE, input, corrected[0]);
            ReflectionUtils.setFloat(Mappings.MOVEMENT_INPUT_FORWARD, input, corrected[1]);
            if (!logged) { logged=true; dev.nebula.client.bridge.NativeBridge.log("MovementHooks: input corrected before vanilla sprint checks"); }
        } catch (Exception e) { dev.nebula.client.bridge.NativeBridge.log("MovementHooks: " + e); }
    }
}
