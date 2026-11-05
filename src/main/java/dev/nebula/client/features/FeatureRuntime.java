package dev.nebula.client.features;

import dev.nebula.client.bridge.NativeBridge;
import dev.nebula.client.game.*;
import dev.nebula.client.game.GameAccess.*;
import dev.nebula.client.game.GameAccess.Stack;
import dev.nebula.client.modules.GameModule;
import dev.nebula.client.modules.Module;
import dev.nebula.client.modules.render.Notifications;
import dev.nebula.client.modules.visuals.HideClans;
import dev.nebula.client.runtime.ClientContext;
import java.nio.*;
import java.util.*;

public final class FeatureRuntime {
    public static final int PLAYER_STRIDE = 40, ITEM_STRIDE = 12;

    private static final class Floats {
        private float[] values = new float[256];
        private int size;

        void addAll(float[] incoming) {
            if (size + incoming.length > values.length)
                values =
                        java.util.Arrays.copyOf(
                                values, Math.max(values.length * 2, size + incoming.length));
            System.arraycopy(incoming, 0, values, size, incoming.length);
            size += incoming.length;
        }

        float[] array() {
            return java.util.Arrays.copyOf(values, size);
        }
    }

    private static final Set<String> VISUALS =
            new HashSet<>(
                    Arrays.asList(
                            "Nametags",
                            "HideClans",
                            "DamageIndicator"));
    private final ClientContext context;
    private Object world;
    private volatile List<Actor> actors = Collections.emptyList();
    private Map<Object, Actor> index = Collections.emptyMap();
    private long lastError, lastSample, lastProjection;
    private int actorMask;

    public FeatureRuntime(ClientContext context) {
        this.context = context;
    }

    public void invalidate() {
        actorMask = 0;
        lastSample = lastProjection = 0;
        publishEmpty();
    }

    public List<Actor> actors() {
        return actors;
    }

    public boolean enabled(String name) {
        Module m = context.modules().getModule(name);
        return m != null && m.isEnabled();
    }

    private boolean needActors() {
        for (String name : VISUALS) if (enabled(name)) return true;
        return false;
    }

    public void beginTick() {
        GameAccess game = context.game();
        game.prepareTick();
        Object nextWorld = game.world();
        if (nextWorld != world) {
            for (Module module : context.modules().getAll())
                if (module instanceof GameModule) ((GameModule) module).resetWorld();
            world = nextWorld;
            lastSample = lastProjection = 0;
            actorMask = 0;
            actors = Collections.emptyList();
            index = Collections.emptyMap();
            context.inventoryLease().clear();
            publishEmpty();
        }
        if (!game.inGame() || !needActors()) {
            actorMask = 0;
            lastSample = 0;
            actors = Collections.emptyList();
            index = Collections.emptyMap();
            return;
        }
        int mask = 0;
        int bit = 1;
        for (String name : VISUALS) {
            if (enabled(name)) mask |= bit;
            bit <<= 1;
        }
        int interval = 50;
        if (mask == actorMask && game.now() - lastSample < interval) return;
        actorMask = mask;
        lastSample = game.now();
        try {
            actors = game.actors();
            Map<Object, Actor> next = new IdentityHashMap<>();
            for (Actor a : actors) next.put(a.handle, a);
            index = next;
        } catch (MappedAccess.Missing error) {
            actors = Collections.emptyList();
            index = Collections.emptyMap();
            if (game.now() - lastError >= 5000 || lastError == 0) {
                lastError = Math.max(1, game.now());
                Notifications.SendNotifications.ERROR("Visuals: " + error.getMessage());
            }
        }
    }

    public void endTick() {}

    public void attacked(Object handle) {
        if (!context.dispatcher().isClientThread()) {
            context.dispatcher().execute(() -> attacked(handle));
            return;
        }
    }

    public boolean hidden(Object handle) {
        Actor actor = index.get(handle);
        if (actor == null) return false;
        Module hide = context.modules().getModule("HideClans");
        return hide instanceof HideClans && ((HideClans) hide).hides(actor.id);
    }

    public boolean suppressName(Object handle) {
        Actor actor = index.get(handle);
        return actor != null && actor.kind.equals("Players") && enabled("Nametags");
    }

    public void renderWorld(float partial) {
        if (!context.dispatcher().isClientThread()
                || !context.game().inGame()
                || !(enabled("Nametags")
                        || enabled("DamageIndicator")
                )) return;
        int interval = 16;
        long now = context.game().now();
        if (now - lastProjection < interval) return;
        lastProjection = now;
        try {
            float[] captured = NativeBridge.captureWorldMatricesNative();
            if (captured == null || captured.length != 36) return;
            float[] model = java.util.Arrays.copyOfRange(captured, 0, 16),
                    projection = java.util.Arrays.copyOfRange(captured, 16, 32);
            int[] viewport = {
                (int) captured[32], (int) captured[33], (int) captured[34], (int) captured[35]
            };
            publish(model, projection, viewport, Math.max(0, Math.min(1, partial)));
        } catch (RuntimeException | LinkageError error) {
            if (context.game().now() - lastError > 5000) {
                lastError = context.game().now();
                Notifications.SendNotifications.ERROR("Visual render: " + error.getMessage());
            }
        }
    }

    private double[] origin() {
        if (!(context.game() instanceof MinecraftAccess)) return new double[3];
        MappedAccess map = ((MinecraftAccess) context.game()).mappings();
        Object mc = dev.nebula.client.mappings.wrapper.client.Minecraft.getMinecraft().getRaw(),
                manager = map.get(mc, "Minecraft", "renderManager");
        return new double[] {
            ((Number) map.get(manager, "RenderManager", "renderPosX")).doubleValue(),
            ((Number) map.get(manager, "RenderManager", "renderPosY")).doubleValue(),
            ((Number) map.get(manager, "RenderManager", "renderPosZ")).doubleValue()
        };
    }

    private boolean visiblyHidden(Actor a) {
        Module hide = context.modules().getModule("HideClans");
        return hide instanceof HideClans && ((HideClans) hide).hides(a.id);
    }

    private void publish(float[] model, float[] projection, int[] viewport, float partial) {
        boolean projected = model != null;
        double[] origin = projected ? origin() : new double[3];
        List<String> names = new ArrayList<>(), itemNames = new ArrayList<>();
        Floats values = new Floats(), items = new Floats();
        Object crosshair = enabled("DamageIndicator") ? context.game().crosshairTarget() : null;
        for (Actor actor : actors)
            if (actor.kind.equals("Players")
                    && (enabled("Nametags")
                            || enabled("DamageIndicator") && actor.handle == crosshair)) {
                if (names.size() / 6 >= 256) break;
                playerRow(
                        actor,
                        true,
                        false,
                        crosshair == actor.handle,
                        projected && !visiblyHidden(actor),
                        partial,
                        origin,
                        model,
                        projection,
                        viewport,
                        names,
                        values);
            }
        itemNames.clear();
        items.addAll(new float[0]);
        try {
            NativeBridge.publishVisualsNative(
                    names.toArray(new String[0]),
                    values.array(),
                    itemNames.toArray(new String[0]),
                    items.array(),
                    projected);
        } catch (UnsatisfiedLinkError ignored) {
        }
    }

    private void playerRow(
            Actor actor,
            boolean online,
            boolean tracked,
            boolean target,
            boolean projected,
            float partial,
            double[] origin,
            float[] model,
            float[] projection,
            int[] viewport,
            List<String> names,
            Floats values) {
        names.add(actor.name);
        names.add(actor.clan);
        for (int i = 0; i < 4; i++)
            names.add(
                    i < actor.armor.size() && actor.armor.get(i) != null
                            ? actor.armor.get(i).name
                            : "");
        float[] screen =
                projected
                        ? Projection.point(
                                actor.px + (actor.x - actor.px) * partial - origin[0],
                                actor.py
                                        + (actor.y - actor.py) * partial
                                        + actor.height
                                        + .3
                                        - origin[1],
                                actor.pz + (actor.z - actor.pz) * partial - origin[2],
                                model,
                                projection,
                                viewport)
                        : null;
        float[] data = new float[PLAYER_STRIDE];
        data[0] = Float.isFinite(actor.health) ? actor.health : 0;
        data[1] = Float.isFinite(actor.maxHealth) ? actor.maxHealth : 0;
        data[2] = (float) actor.distance;
        data[3] = screen == null ? 0 : screen[0];
        data[4] = screen == null ? 0 : screen[1];
        data[5] = screen == null ? 0 : 1;
        data[6] = online ? 1 : 0;
        data[7] = 0;
        data[8] = actor.id;
        data[9] = tracked ? 1 : 0;
        for (int i = 0; i < 4; i++) {
            Stack stack = i < actor.armor.size() ? actor.armor.get(i) : null;
            int offset = 10 + i * 6;
            data[offset] = stack == null ? -1 : stack.durability();
            float[] icon = projected ? context.game().icon(stack) : new float[5];
            System.arraycopy(icon, 0, data, offset + 1, 5);
        }
        data[34] = target ? 1 : 0;
        if (projected && target && enabled("DamageIndicator") && actor.handle != null)
            System.arraycopy(context.game().skin(actor), 0, data, 35, 5);
        values.addAll(data);
    }

    private void publishEmpty() {
        try {
            NativeBridge.publishVisualsNative(
                    new String[0], new float[0], new String[0], new float[0], true);
        } catch (UnsatisfiedLinkError ignored) {
        }
    }
}
