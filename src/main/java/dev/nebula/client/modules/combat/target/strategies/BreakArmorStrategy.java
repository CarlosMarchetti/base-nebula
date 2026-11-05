package dev.nebula.client.modules.combat.target.strategies;

import dev.nebula.client.mappings.wrapper.client.entity.EntityPlayerSP;
import dev.nebula.client.mappings.wrapper.client.multiplayer.WorldClient;
import dev.nebula.client.modules.combat.KillAura;
import dev.nebula.client.modules.combat.KillAura.Candidate;
import dev.nebula.client.modules.combat.target.ITargetStrategy;
import dev.nebula.client.modules.combat.target.TargetManager;
import dev.nebula.client.modules.combat.target.TargetMode;
import dev.nebula.client.utils.combat.ArmorUtils;
import java.util.List;

public class BreakArmorStrategy implements ITargetStrategy {

    @Override
    public TargetMode getMode() {
        return TargetMode.BREAK_ARMOR;
    }

    @Override
    public Candidate select(
            EntityPlayerSP localPlayer,
            WorldClient world,
            List<Candidate> candidates,
            TargetManager manager,
            KillAura aura) {
        String activeBreakTarget = manager.getBreakArmorTargetName();

        if (!activeBreakTarget.isEmpty()) {
            for (Candidate c : candidates) {
                if (TargetManager.targetNamesMatch(c.entity.getName(), activeBreakTarget)) {
                    if (manager.shouldKeepBreakArmorTarget(c.entity)) {
                        return c;
                    }
                    break;
                }
            }
        }

        manager.resetBreakArmorTracking();
        Candidate best = null;
        float bestScore = Float.MAX_VALUE;

        for (Candidate c : candidates) {
            float score =
                    ArmorUtils.getBreakArmorScore(c.entity.getHandle())
                            + (float) (c.distance * 0.001);
            if (score < bestScore) {
                bestScore = score;
                best = c;
            }
        }

        if (best != null) {
            manager.trackBreakArmorTarget(best.entity);
        }

        return best;
    }
}
