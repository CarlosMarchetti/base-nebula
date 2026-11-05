package dev.nebula.client.modules.combat;

import dev.nebula.client.Main;
import dev.nebula.client.bridge.NativeBridge;
import dev.nebula.client.event.EventManager;
import dev.nebula.client.event.EventTarget;
import dev.nebula.client.event.impl.player.PostMotionEvent;
import dev.nebula.client.event.impl.player.PreMotionEvent;
import dev.nebula.client.gui.settings.Setting;
import dev.nebula.client.gui.settings.impl.*;
import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.mappings.wrapper.client.Minecraft;
import dev.nebula.client.mappings.wrapper.client.entity.EntityPlayerSP;
import dev.nebula.client.mappings.wrapper.client.multiplayer.PlayerControllerMP;
import dev.nebula.client.mappings.wrapper.client.multiplayer.WorldClient;
import dev.nebula.client.mappings.wrapper.entity.Entity;
import dev.nebula.client.mappings.wrapper.entity.EntityLivingBase;
import dev.nebula.client.mappings.wrapper.entity.player.EntityPlayer;
import dev.nebula.client.mappings.wrapper.util.AxisAlignedBB;
import dev.nebula.client.modules.Category;
import dev.nebula.client.modules.Module;
import dev.nebula.client.modules.combat.target.TargetManager;
import dev.nebula.client.modules.combat.target.TargetMode;
import dev.nebula.client.utils.*;
import dev.nebula.client.utils.RotationUtils.Rotation;
import dev.nebula.client.utils.math.MathHelper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import org.lwjgl.input.Keyboard;

public final class KillAura extends Module {

    private final TargetManager targetManager = new TargetManager();
    private final Random random = new Random();

    private List<Candidate> candidates = Collections.emptyList();
    private Candidate target;
    private Rotation requested, reported;
    private Object worldHandle;
    private boolean prepared;
    private boolean acquiring = true;
    private long nextAttackAt, lastMotionAt, nextDiagnostic;
    private String state = "disabled";

    public KillAura() {
        super(
                "KillAura",
                "Silent aim with advanced Target system",
                Category.COMBAT,
                Keyboard.KEY_R);
    }

    @Override
    public void setup() {
        add(
                new ComboBoxSetting(
                        "Mode", this, AttackMode.SINGLE.getDisplayName(), AttackMode.getNames()));
        add(new DoubleSetting("Max Targets", this, 3, 1, 8));
        add(new DoubleSetting("Min CPS", this, 8, 1, 16));
        add(new DoubleSetting("CPS", this, 12, 1, 16));
        add(new DoubleSetting("Range", this, 3.0, 2.5, 4.5));
        add(new DoubleSetting("Scan Extra", this, 1.0, 0, 3));
        add(new DoubleSetting("FOV", this, 360, 30, 360));
        add(new DoubleSetting("Turn Speed", this, 90, 10, 180));
        add(new DoubleSetting("Transition Speed", this, 12, 1, 45));
        add(new BooleanSetting("Lazy Rotation", this, true));
        add(new BooleanSetting("Players Only", this, true));
        
        // A prioridade de alvo pertence ao KillAura; não exige um módulo separado.
        add(new ComboBoxSetting(
                "Target Mode", this, TargetMode.LOW_ARMOR.getDisplayName(), TargetMode.getNames()));

        add(new KeyBindSetting("Keybind", this, getKey()));
    }

    private void add(Setting setting) {
        Main.getSettingsManager().addSetting(setting);
    }

    public void onServerCorrection() {
        requested = reported = null;
        prepared = false;
        acquiring = true;
    }

    public TargetManager getTargetManager() {
        return targetManager;
    }

    @Override
    public void onEnable() {
        clear();
        EntityPlayerSP seedPlayer = Minecraft.getPlayer();
        if (seedPlayer != null) {
            reported = RotationUtils.serverOrCamera(seedPlayer);
            worldHandle = Minecraft.getWorld() == null ? null : Minecraft.getWorld().getHandle();
        }
        acquiring = true;
        targetManager.reset();
        EventManager.getInstance().register(this);
        state = "acquiring target";
        NativeBridge.log(
                "KillAura enabled: always silent; mode="
                        + getValueString("Mode")
                        + "; targetMode="
                        + getValueString("Target Mode"));
    }

    @Override
    public void onDisable() {
        EventManager.getInstance().unregister(this);
        if (Minecraft.getWorld() != null) {
            targetManager.restoreAllHitboxes(Minecraft.getWorld());
        }
        clear();
        RotationUtils.releaseTargetRotation((float) getValueDouble("Transition Speed"));
        targetManager.reset();
    }

    private void clear() {
        candidates = Collections.emptyList();
        target = null;
        requested = reported = null;
        worldHandle = null;
        prepared = false;
        nextAttackAt = lastMotionAt = nextDiagnostic = 0;
        acquiring = true;
    }

    @Override
    public void onTick() {
        EntityPlayerSP player = Minecraft.getPlayer();
        WorldClient world = Minecraft.getWorld();
        if (player == null || world == null || player.isDead()) {
            targetManager.restoreAllHitboxes(world);
            clear();
            RotationUtils.resetSession();
            return;
        }

        if (worldHandle != world.getHandle()) {
            clear();
            targetManager.reset();
            worldHandle = world.getHandle();
            RotationUtils.resetSession();
            reported = RotationUtils.serverOrCamera(player);
            acquiring = true;
        }

        candidates = collect(player, world);
        Candidate previous = target;
        target = targetManager.selectTarget(player, world, candidates, this);
        if (target != null
                && (previous == null || previous.entity.getHandle() != target.entity.getHandle()))
            acquiring = true;
        targetManager.restoreAllHitboxes(world);

        if (target == null) {
            requested = null;
            if (RotationUtils.getTargetRotation() != null && !RotationUtils.isReturning())
                RotationUtils.releaseTargetRotation((float) getValueDouble("Transition Speed"));
            reported = RotationUtils.serverOrCamera(player);
            state = "no eligible target";
        } else {
            attackReadyTargets(player);
            requested = aim(player, target);
            RotationUtils.setTargetRotation(requested);
        }
        diagnostic();
    }

    @EventTarget
    public void onPreMotion(PreMotionEvent event) {
        prepared = false;
        lastMotionAt = System.currentTimeMillis();
        EntityPlayerSP player = Minecraft.getPlayer();
        if (!isEnabled() || player == null || target == null || !valid(player, target.entity))
            return;
        if (requested == null) return;

        event.setYaw(requested.getYaw());
        event.setPitch(requested.getPitch());
        prepared = true;
    }

    @EventTarget
    public void onPostMotion(PostMotionEvent event) {
        if (!isEnabled() || !prepared) return;
        prepared = false;
        reported = requested;
    }

    private void attackReadyTargets(EntityPlayerSP player) {
        if (Main.getContext().guiInput().isCaptured()
                || Main.getContext().inventoryLease().heldByOther(this)) {
            state = "inventory action";
            return;
        }
        if (reported == null) {
            state = "waiting for first reported rotation";
            return;
        }
        if (!valid(player, target.entity)) {
            state = "target invalidated";
            return;
        }
        long now = System.currentTimeMillis();
        if (now < nextAttackAt) {
            state = "click cooldown";
            return;
        }

        boolean isMulti = AttackMode.MULTI.getDisplayName().equals(getValueString("Mode"));
        List<Candidate> attackList = isMulti ? candidates : Collections.singletonList(target);
        int sent = 0;
        int limit = isMulti ? (int) getValueDouble("Max Targets") : 1;

        for (Candidate old : attackList) {
            if (!valid(player, old.entity)) continue;
            Candidate current = candidate(player, old.entity);
            if (!intersects(player, reported, current.box, getValueDouble("Range"))) {
                state = "turning / target outside reported ray";
                continue;
            }
            if (!visible(player, current.entity)) {
                state = "target obstructed";
                continue;
            }
            try {
                PlayerControllerMP controller = Minecraft.getPlayerControllerMP();
                if (controller == null || controller.getHandle() == null) {
                    state = "vanilla controller unavailable";
                    break;
                }
                player.swingItem();
                controller.attackEntity(current.entity);

                targetManager.onEntityAttacked(player, Minecraft.getWorld(), current.entity);

                sent++;
                state = "attack sent to " + current.entity.getName();
            } catch (RuntimeException e) {
                state = "attack failed: " + e;
                break;
            }
            if (sent >= limit) break;
        }

        if (sent > 0) nextAttackAt = now + delay();
        diagnostic();
    }

    private long delay() {
        double max = Math.max(1, getValueDouble("CPS"));
        double min = Math.min(max, Math.max(1, getValueDouble("Min CPS")));
        return Math.max(1, Math.round(1000.0 / (min + random.nextDouble() * (max - min))));
    }

    private Rotation aim(EntityPlayerSP player, Candidate candidate) {
        Rotation base =
                reported == null
                        ? new Rotation(player.getRotationYaw(), player.getRotationPitch())
                        : reported;
        if (getValueBoolean("Lazy Rotation")
                && intersects(player, base, candidate.box, getValueDouble("Range"))) {
            return base;
        }
        double[] box = candidate.box;
        double x = (box[0] + box[3]) * 0.5 - player.getPosX();
        double y =
                Math.max(
                                box[1] + 0.1,
                                Math.min(
                                        player.getPosY() + player.getActualEyeHeight(),
                                        box[4] - 0.1))
                        - player.getPosY()
                        - player.getActualEyeHeight();
        double z = (box[2] + box[5]) * 0.5 - player.getPosZ();
        float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(y, Math.sqrt(x * x + z * z)));
        float speed = (float) getValueDouble("Turn Speed");
        if (acquiring) {
            speed = Math.min(speed, (float) getValueDouble("Transition Speed"));
            if (Math.abs(MathHelper.wrapAngleTo180_float(yaw - base.getYaw())) <= speed
                    && Math.abs(pitch - base.getPitch()) <= speed) acquiring = false;
        }
        float nextYaw =
                base.getYaw()
                        + MathHelper.clamp_float(
                                MathHelper.wrapAngleTo180_float(yaw - base.getYaw()),
                                -speed,
                                speed);
        float nextPitch =
                base.getPitch() + MathHelper.clamp_float(pitch - base.getPitch(), -speed, speed);
        return RotationUtils.applySensitivityPatch(new Rotation(nextYaw, nextPitch), base);
    }

    private boolean intersects(EntityPlayerSP player, Rotation rotation, double[] b, double range) {
        return rotation != null
                && AimGeometry.intersects(
                        player.getPosX(),
                        player.getPosY() + player.getActualEyeHeight(),
                        player.getPosZ(),
                        rotation.getYaw(),
                        rotation.getPitch(),
                        range,
                        b[0],
                        b[1],
                        b[2],
                        b[3],
                        b[4],
                        b[5]);
    }

    private boolean valid(EntityPlayerSP player, Entity e) {
        return e != null
                && e.getHandle() != player.getHandle()
                && EntityLivingBase.isInstance(e)
                && !e.isDead()
                && !e.isInvisible()
                && new EntityLivingBase(e.getHandle()).getHealth() > 0
                && (!getValueBoolean("Players Only") || EntityPlayer.isInstance(e))
                && !TargetManager.isAlly(player, e);
    }

    private List<Candidate> collect(EntityPlayerSP player, WorldClient world) {
        List<Candidate> out = new ArrayList<>();
        try {
            Object loaded =
                    ReflectionUtils.getFieldValue(
                            Mappings.WORLD_LOADED_ENTITY_LIST, world.getHandle());
            if (!(loaded instanceof List)) {
                state = "loaded entity mapping unavailable";
                return out;
            }
            for (Object raw : ((List<?>) loaded).toArray()) {
                Entity entity = new Entity(raw);
                if (raw == null || !valid(player, entity)) continue;
                Candidate c = candidate(player, entity);
                if (c.distance > getValueDouble("Range") + getValueDouble("Scan Extra")) continue;
                if (c.direction > getValueDouble("FOV") * 0.5) continue;
                out.add(c);
            }
            out.sort(
                    Comparator.comparingDouble((Candidate c) -> c.distance)
                            .thenComparingInt(c -> c.entity.getEntityId()));
        } catch (Exception e) {
            state = "target scan failed: " + e;
        }
        return out;
    }

    private Candidate candidate(EntityPlayerSP player, Entity entity) {
        double[] b = bounds(entity);
        double x = player.getPosX(),
                y = player.getPosY() + player.getActualEyeHeight(),
                z = player.getPosZ();
        double dx = Math.max(b[0] - x, Math.max(0, x - b[3]));
        double dy = Math.max(b[1] - y, Math.max(0, y - b[4]));
        double dz = Math.max(b[2] - z, Math.max(0, z - b[5]));
        float yaw =
                (float) Math.toDegrees(Math.atan2((b[2] + b[5]) * 0.5 - z, (b[0] + b[3]) * 0.5 - x))
                        - 90f;
        return new Candidate(
                entity,
                b,
                Math.sqrt(dx * dx + dy * dy + dz * dz),
                Math.abs(MathHelper.wrapAngleTo180_float(yaw - player.getRotationYaw())),
                new EntityLivingBase(entity.getHandle()).getHealth());
    }

    private double[] bounds(Entity entity) {
        try {
            AxisAlignedBB box = entity.getBoundingBox();
            if (box != null && box.getHandle() != null) {
                double[] b = {
                    box.getMinX(),
                    box.getMinY(),
                    box.getMinZ(),
                    box.getMaxX(),
                    box.getMaxY(),
                    box.getMaxZ()
                };
                boolean finite = true;
                for (double value : b) finite &= Double.isFinite(value);
                if (finite && b[3] > b[0] && b[4] > b[1] && b[5] > b[2]) return b;
            }
        } catch (RuntimeException ignored) {
        }
        double width = 0.6, height = Math.max(0.1, entity.getEntityHeight());
        try {
            Field f = ReflectionUtils.getField(entity.getHandle().getClass(), "width");
            if (f != null) width = ((Number) f.get(entity.getHandle())).doubleValue();
        } catch (Exception ignored) {
        }
        double x = entity.getPosX(), y = entity.getPosY(), z = entity.getPosZ();
        return new double[] {
            x - width * 0.5, y, z - width * 0.5, x + width * 0.5, y + height, z + width * 0.5
        };
    }

    private boolean visible(EntityPlayerSP player, Entity entity) {
        try {
            Method method =
                    ReflectionUtils.getMethod(
                            player.getHandle().getClass(),
                            "canEntityBeSeen",
                            Mappings.ENTITY_CLASS);
            return Boolean.TRUE.equals(method.invoke(player.getHandle(), entity.getHandle()));
        } catch (Exception e) {
            state = "visibility mapping unavailable";
            return false;
        }
    }

    private void diagnostic() {
        long now = System.currentTimeMillis();
        if (now < nextDiagnostic) return;
        nextDiagnostic = now + 2000;
        String motion = lastMotionAt == 0 ? "not received" : (now - lastMotionAt) + "ms ago";
        String targetInfo = (target != null) ? target.entity.getName() : "none";
        NativeBridge.log(
                "KillAura: "
                        + state
                        + "; target="
                        + targetInfo
                        + "; totalCandidates="
                        + candidates.size()
                        + "; walking="
                        + motion);
    }

    public static final class Candidate {
        public final Entity entity;
        public final double[] box;
        public final double distance, direction, health;

        public Candidate(
                Entity entity, double[] box, double distance, double direction, double health) {
            this.entity = entity;
            this.box = box;
            this.distance = distance;
            this.direction = direction;
            this.health = health;
        }
    }
}
