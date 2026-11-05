package dev.nebula.client.modules.combat.target;

import dev.nebula.client.Main;
import dev.nebula.client.mappings.wrapper.client.entity.EntityPlayerSP;
import dev.nebula.client.mappings.wrapper.client.multiplayer.WorldClient;
import dev.nebula.client.mappings.wrapper.entity.Entity;
import dev.nebula.client.modules.combat.KillAura;
import dev.nebula.client.modules.combat.KillAura.Candidate;
import dev.nebula.client.modules.combat.target.strategies.*;
import dev.nebula.client.utils.PlayerUtils;
import dev.nebula.client.utils.combat.ArmorUtils;
import java.lang.reflect.Field;
import java.util.*;

public class TargetManager {
    private final Map<TargetMode, ITargetStrategy> strategies = new EnumMap<>(TargetMode.class);
    private final Map<Object, float[]> originalHitboxMap = new IdentityHashMap<>();
    private Object hitboxWorld, breakHandle;
    private String lockedTargetName = "", breakArmorTargetName = "";
    private int breakArmorMask;
    private final int[] breakRemaining = new int[4];

    public TargetManager() {
        registerStrategy(new LowArmorStrategy());
        registerStrategy(new BreakArmorStrategy());
        registerStrategy(new HealthStrategy());
    }

    public void registerStrategy(ITargetStrategy strategy) {
        strategies.put(strategy.getMode(), strategy);
    }

    public synchronized void reset() {
        lockedTargetName = "";
        resetBreakArmorTracking();
    }

    public synchronized String getLockedTargetName() {
        return lockedTargetName;
    }

    public synchronized String getBreakArmorTargetName() {
        return breakArmorTargetName;
    }

    public synchronized Candidate selectTarget(
            EntityPlayerSP local, WorldClient world, List<Candidate> candidates, KillAura aura) {
        if (candidates == null || candidates.isEmpty()) {
            reset();
            return null;
        }
        Candidate chosen;
        chosen =
                strategies
                        .get(TargetMode.fromDisplayName(aura.getValueString("Target Mode")))
                        .select(local, world, candidates, this, aura);
        lockedTargetName = chosen == null || chosen.entity == null ? "" : chosen.entity.getName();
        return chosen;
    }

    public void onEntityAttacked(EntityPlayerSP local, WorldClient world, Entity target) {}

    public boolean shouldKeepBreakArmorTarget(Entity target) {
        if (target == null || target.getHandle() != breakHandle || target.isDead()) return false;
        int mask = ArmorUtils.getEquippedArmorMask(target.getHandle());
        if (mask == 0 || (mask & breakArmorMask) != breakArmorMask) return false;
        for (int i = 0; i < 4; i++) {
            Object stack = ArmorUtils.getArmorInSlot(target.getHandle(), i);
            int remaining =
                    Math.max(
                            0,
                            ArmorUtils.getItemMaxDamage(stack) - ArmorUtils.getItemDamage(stack));
            if (remaining > breakRemaining[i])
                return false; 
            breakRemaining[i] = remaining;
        }
        return true;
    }

    public void trackBreakArmorTarget(Entity target) {
        if (target == null) {
            resetBreakArmorTracking();
            return;
        }
        breakHandle = target.getHandle();
        breakArmorTargetName = target.getName();
        breakArmorMask = ArmorUtils.getEquippedArmorMask(breakHandle);
        for (int i = 0; i < 4; i++) {
            Object stack = ArmorUtils.getArmorInSlot(breakHandle, i);
            breakRemaining[i] =
                    Math.max(
                            0,
                            ArmorUtils.getItemMaxDamage(stack) - ArmorUtils.getItemDamage(stack));
        }
    }

    public void resetBreakArmorTracking() {
        breakHandle = null;
        breakArmorTargetName = "";
        breakArmorMask = 0;
    }

    public static boolean targetNamesMatch(String left, String right) {
        return stripMinecraftFormatting(left).equalsIgnoreCase(stripMinecraftFormatting(right));
    }

    public static String normalizeBrowseTag(String text) {
        return stripMinecraftFormatting(text)
                .replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase(Locale.ROOT);
    }

    public static String stripMinecraftFormatting(String text) {
        return text == null ? "" : text.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
    }

    public void manageHitboxes(
            EntityPlayerSP localPlayer,
            WorldClient world,
            Candidate chosenTarget,
            boolean zeroHitboxes) {
        if (world == null || world.getHandle() == null) return;
        if (hitboxWorld != world.getHandle()) {
            restoreAllHitboxes(null);
            hitboxWorld = world.getHandle();
        }
        if (!zeroHitboxes && originalHitboxMap.isEmpty()) return;
        try {
            List<Entity> players = world.getPlayerEntities();
            if (players == null || players.isEmpty()) return;

            String chosenName = (chosenTarget != null) ? chosenTarget.entity.getName() : "";

            for (Entity p : players) {
                if (p == null || p.getHandle() == null) continue;
                if (localPlayer != null && p.getHandle() == localPlayer.getHandle()) continue;

                String name = p.getName();
                if (!zeroHitboxes || chosenTarget == null) {
                    restoreHitbox(p);
                } else {
                    if (targetNamesMatch(name, chosenName)) {
                        restoreHitbox(p);
                    } else {
                        zeroHitbox(p);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void zeroHitbox(Entity p) {
        try {
            Field wf = ArmorUtils.findField(p.getHandle().getClass(), "width");
            Field hf = ArmorUtils.findField(p.getHandle().getClass(), "height");
            if (wf != null && hf != null) {
                Object id = p.getHandle();
                if (!originalHitboxMap.containsKey(id)) {
                    originalHitboxMap.put(
                            id,
                            new float[] {wf.getFloat(p.getHandle()), hf.getFloat(p.getHandle())});
                }
                wf.setFloat(p.getHandle(), 0.0f);
                hf.setFloat(p.getHandle(), 0.0f);
            }
        } catch (Throwable ignored) {
        }
    }

    private void restoreHitbox(Entity p) {
        try {
            float[] orig = originalHitboxMap.remove(p.getHandle());
            if (orig == null) return;
            Field wf = ArmorUtils.findField(p.getHandle().getClass(), "width");
            Field hf = ArmorUtils.findField(p.getHandle().getClass(), "height");
            if (wf != null && hf != null) {
                float w = orig[0];
                float h = orig[1];
                wf.setFloat(p.getHandle(), w);
                hf.setFloat(p.getHandle(), h);
            }
        } catch (Throwable ignored) {
        }
    }

    public void restoreAllHitboxes(WorldClient world) {
        for (Object entity : new ArrayList<>(originalHitboxMap.keySet()))
            restoreHitbox(new Entity(entity));
        originalHitboxMap.clear();
    }

    public static boolean isAlly(EntityPlayerSP localPlayer, Entity other) {
        if (other == null || other.getHandle() == null) return false;
        if (localPlayer != null && other.getHandle() == localPlayer.getHandle()) return true;

        if (Main.getClanManager() != null && Main.getClanManager().isAlly(other.getHandle())) {
            return true;
        }

        String localClan =
                PlayerUtils.getClan(localPlayer != null ? localPlayer.getHandle() : null);
        String otherClan = PlayerUtils.getClan(other.getHandle());
        if (!localClan.isEmpty() && !otherClan.isEmpty() && localClan.equalsIgnoreCase(otherClan)) {
            return true;
        }

        return false;
    }
}
