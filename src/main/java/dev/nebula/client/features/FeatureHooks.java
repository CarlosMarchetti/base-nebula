package dev.nebula.client.features;

import dev.nebula.client.Main;
import dev.nebula.client.runtime.ClientContext;
import dev.nebula.client.utils.Log;

public final class FeatureHooks {
    private static long lastError;

    private FeatureHooks() {}

    public static void serverRotationReady() {
        ClientContext context = Main.getContext();
        if (context == null) return;
        context.dispatcher()
                .execute(
                        () -> {
                            dev.nebula.client.utils.RotationUtils.resetSession();
                            dev.nebula.client.modules.Module module = context.modules().getModule("KillAura");
                            if (module instanceof dev.nebula.client.modules.combat.KillAura)
                                ((dev.nebula.client.modules.combat.KillAura) module).onServerCorrection();
                            context.features().invalidate();
                        });
    }

    public static void movementInputReady(Object player) {
        ClientContext c = Main.getContext();
        if (c == null || c.game().player() != player) return;
        dev.nebula.client.modules.Module sprint = c.modules().getModule("Sprint"),
                jump = c.modules().getModule("JumpReset");
        if ((sprint == null || !sprint.isEnabled()) && (jump == null || !jump.isEnabled())) return;
        try {
            dev.nebula.client.game.GameAccess.MovementState state = c.game().movementState();
            if (sprint instanceof dev.nebula.client.modules.movement.Sprint && sprint.isEnabled())
                ((dev.nebula.client.modules.movement.Sprint) sprint).input(state);
            if (jump instanceof dev.nebula.client.modules.movement.JumpReset && jump.isEnabled())
                ((dev.nebula.client.modules.movement.JumpReset) jump).input(state);
        } catch (dev.nebula.client.game.MappedAccess.Missing error) {
            long now = System.currentTimeMillis();
            if (now - lastError > 5000) {
                lastError = now;
                dev.nebula.client.modules.render.Notifications.SendNotifications.ERROR(
                        "Movement: " + error.getMessage());
            }
        }
    }

    public static void beforeAttackClick() {
        ClientContext c = Main.getContext();
        if (c == null) return;
        dev.nebula.client.modules.Module module = c.modules().getModule("NoHitDelay");
        if (module != null && module.isEnabled() && !c.game().guiOpen()) c.game().clearHitDelay();
    }

    public static void inventoryReady(Object packet, int kind) {
        ClientContext c = Main.getContext();
        if (c == null) return;
        try {
            c.game().inventoryPacket(packet, kind);
        } catch (RuntimeException error) {
            dev.nebula.client.game.InventorySync sync = c.game().inventorySync();
            if (sync != null && sync.active())
                sync.reject("Falha ao conferir pacote de inventario");
        }
    }

    public static boolean blockCamera(Object entity) {
        ClientContext c = Main.getContext();
        return c != null && c.guiInput().blocksCamera(entity);
    }

    public static boolean hide(Object entity) {
        ClientContext c = Main.getContext();
        return c != null && c.features().hidden(entity);
    }

    public static boolean hideName(Object entity) {
        ClientContext c = Main.getContext();
        return c != null && c.features().suppressName(entity);
    }

    public static void attacked(Object entity) {
        ClientContext c = Main.getContext();
        if (c != null) c.features().attacked(entity);
    }

    public static void worldRender(float partial) {
        ClientContext c = Main.getContext();
        if (c == null) return;
        try {
            c.features().renderWorld(partial);
        } catch (RuntimeException error) {
            long now = System.currentTimeMillis();
            if (now - lastError > 5000) {
                lastError = now;
                Log.error("World overlay failed: " + error);
            }
        }
    }
}
