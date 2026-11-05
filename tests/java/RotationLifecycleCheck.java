import dev.nebula.client.modules.combat.KillAura;
import dev.nebula.client.utils.*;

public class RotationLifecycleCheck {
    static void check(boolean b, String text) {
        if (!b) throw new AssertionError(text);
    }

    static float yawDifference(float a, float b) {
        float delta = (a - b) % 360;
        if (delta > 180) delta -= 360;
        if (delta < -180) delta += 360;
        return Math.abs(delta);
    }

    public static void main(String[] args) throws Exception {
        CombatTickCheck.main(new String[0]);
        CombatTickCheck.Living player = CombatTickCheck.Game.instance.player;
        player.rotationYaw = -120;
        player.rotationPitch = 45;
        RotationUtils.recordServerRotation(player, new RotationUtils.Rotation(110, -20));
        RotationUtils.setTargetRotation(new RotationUtils.Rotation(110, -20));
        RotationUtils.releaseTargetRotation(12);
        int steps = 0;
        while (RotationUtils.isReturning() && steps++ < 40) {
            RotationUtils.Rotation before = RotationUtils.getCurrentRotation();
            RotationUtils.advanceRelease();
            RotationUtils.Rotation next = RotationUtils.getTargetRotation();
            check(
                    next != null
                            && yawDifference(next.getYaw(), before.getYaw()) <= 12.001f
                            && Math.abs(next.getPitch() - before.getPitch()) <= 12.001f,
                    "Release jumped beyond bounded step");
            MotionHooks.begin(player);
            float yaw = MotionHooks.yaw(player, player.rotationYaw),
                    pitch = MotionHooks.pitch(player, player.rotationPitch);
            check(
                    yawDifference(yaw, before.getYaw()) <= 12.001f
                            && Math.abs(pitch - before.getPitch()) <= 12.001f,
                    "Walking hook discarded release transition");
            MotionHooks.end(player);
            check(player.rotationYaw == -120 && player.rotationPitch == 45, "Release moved camera");
        }
        check(
                !RotationUtils.isReturning()
                        && RotationUtils.getTargetRotation() == null
                        && steps > 1,
                "Release failed to converge/retained movement correction");
        float lastYaw = RotationUtils.getCurrentRotation().getYaw();
        MotionHooks.begin(player);
        float ordinary = MotionHooks.yaw(player, player.rotationYaw);
        MotionHooks.pitch(player, player.rotationPitch);
        MotionHooks.end(player);
        check(
                Math.abs(ordinary - lastYaw) < .01f,
                "First vanilla walking value jumped by a full rotation after release");
        player.rotationYaw = 720 + 179;
        RotationUtils.recordServerRotation(player, new RotationUtils.Rotation(720 + 179, 0));
        RotationUtils.releaseTargetRotation(12);
        player.rotationYaw = 720 - 179;
        float previousRaw = RotationUtils.getCurrentRotation().getYaw();
        RotationUtils.advanceRelease();
        MotionHooks.begin(player);
        float nextRaw = MotionHooks.yaw(player, player.rotationYaw);
        MotionHooks.pitch(player, player.rotationPitch);
        MotionHooks.end(player);
        check(
                Math.abs(nextRaw - previousRaw) <= 12.001f,
                "Accumulated yaw wrapped across the long numeric path");
        player.rotationYaw = -120;
        RotationUtils.recordServerRotation(player, new RotationUtils.Rotation(100, 0));
        RotationUtils.releaseTargetRotation(12);
        RotationUtils.advanceRelease();
        MotionHooks.begin(player);
        MotionHooks.yaw(player, player.rotationYaw);
        MotionHooks.pitch(player, player.rotationPitch);
        MotionHooks.end(player);
        KillAura aura = new KillAura();
        aura.initializeSettings();
        aura.enable();
        RotationUtils.Rotation before = RotationUtils.getCurrentRotation();
        aura.onTick();
        RotationUtils.Rotation desired = RotationUtils.getTargetRotation();
        check(
                desired != null && yawDifference(desired.getYaw(), before.getYaw()) <= 12.001f,
                "Re-enable reset from camera instead of last walking rotation");
        check(player.rotationYaw == -120 && player.rotationPitch == 45, "Re-enable moved camera");
        aura.disable();
        RotationUtils.resetSession();
        RotationUtils.Rotation wrap =
                RotationUtils.stepToward(
                        new RotationUtils.Rotation(179, 0), new RotationUtils.Rotation(-179, 0), 5);
        check(yawDifference(wrap.getYaw(), -179) < .001, "Rotation crossed the long yaw path");
        System.out.println(
                "PASS bounded return, last walking state, fast re-enable, yaw wrap, no retained correction and camera preservation");
    }
}
