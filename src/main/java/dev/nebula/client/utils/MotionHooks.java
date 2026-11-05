package dev.nebula.client.utils;

import dev.nebula.client.event.EventManager;
import dev.nebula.client.event.impl.player.PostMotionEvent;
import dev.nebula.client.event.impl.player.PreMotionEvent;
import dev.nebula.client.mappings.wrapper.client.entity.EntityPlayerSP;

public final class MotionHooks {
    private static final ThreadLocal<Context> ACTIVE = new ThreadLocal<>();
    private static boolean logged;

    private static final class Context {
        final Object player;
        final PreMotionEvent event;
        boolean yawRead, pitchRead;

        Context(Object player, PreMotionEvent event) {
            this.player = player;
            this.event = event;
        }
    }

    private MotionHooks() {}

    public static void begin(Object raw) {
        ACTIVE.remove();
        try {
            EntityPlayerSP p = new EntityPlayerSP(raw);
            RotationUtils.Rotation baseline = RotationUtils.cameraBaseline(p);
            PreMotionEvent event =
                    new PreMotionEvent(
                            p.getPosX(),
                            p.getPosY(),
                            p.getPosZ(),
                            baseline.getYaw(),
                            baseline.getPitch(),
                            p.isOnGround(),
                            p.isSprinting(),
                            p.isSneaking());
            ACTIVE.set(new Context(raw, event));
            if (!logged) {
                logged = true;
                dev.nebula.client.bridge.NativeBridge.log("MotionHooks: walking hook running");
            }
            if (RotationUtils.isReturning() && RotationUtils.getTargetRotation() != null) {
                event.setYaw(RotationUtils.getTargetRotation().getYaw());
                event.setPitch(RotationUtils.getTargetRotation().getPitch());
            }
            EventManager.getInstance().call(event);
        } catch (Throwable t) {
            ACTIVE.remove();
            dev.nebula.client.bridge.NativeBridge.log("MotionHooks.begin: " + t);
        }
    }

    public static float yaw(Object raw, float camera) {
        Context c = ACTIVE.get();
        if (c == null || c.player != raw || c.event.isCancelled()) return camera;
        c.yawRead = true;
        return c.event.getYaw();
    }

    public static float pitch(Object raw, float camera) {
        Context c = ACTIVE.get();
        if (c == null || c.player != raw || c.event.isCancelled()) return camera;
        c.pitchRead = true;
        return c.event.getPitch();
    }

    public static void end(Object raw) {
        Context c = ACTIVE.get();
        try {
            if (c != null
                    && c.player == raw
                    && !c.event.isCancelled()
                    && c.yawRead
                    && c.pitchRead) {
                RotationUtils.recordServerRotation(
                        raw, new RotationUtils.Rotation(c.event.getYaw(), c.event.getPitch()));
                EventManager.getInstance().call(new PostMotionEvent());
            }
        } finally {
            ACTIVE.remove();
        }
    }
}
