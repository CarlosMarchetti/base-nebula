package dev.nebula.client.modules.combat.target.strategies;

import dev.nebula.client.mappings.wrapper.client.entity.EntityPlayerSP;
import dev.nebula.client.mappings.wrapper.client.multiplayer.WorldClient;
import dev.nebula.client.modules.combat.KillAura;
import dev.nebula.client.modules.combat.KillAura.Candidate;
import dev.nebula.client.modules.combat.target.ITargetStrategy;
import dev.nebula.client.modules.combat.target.TargetManager;
import dev.nebula.client.modules.combat.target.TargetMode;
import java.util.List;

public class HealthStrategy implements ITargetStrategy {

    @Override
    public TargetMode getMode() {
        return TargetMode.HEALTH;
    }

    @Override
    public Candidate select(
            EntityPlayerSP localPlayer,
            WorldClient world,
            List<Candidate> candidates,
            TargetManager manager,
            KillAura aura) {
        manager.resetBreakArmorTracking();
        Candidate best = null;
        double bestHealth = Double.MAX_VALUE;

        for (Candidate c : candidates) {
            double score = c.health + (c.distance * 0.001);
            if (score < bestHealth) {
                bestHealth = score;
                best = c;
            }
        }

        return best;
    }
}
