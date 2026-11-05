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

public class LowArmorStrategy implements ITargetStrategy {

    @Override
    public TargetMode getMode() {
        return TargetMode.LOW_ARMOR;
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
        float bestScore = Float.MAX_VALUE;

        for (Candidate c : candidates) {
            float durability = ArmorUtils.getTotalArmorDurability(c.entity.getHandle());
            float score =
                    ArmorUtils.getEquippedPiecesCount(c.entity.getHandle()) * 1000000f
                            + durability
                            + (float) (c.distance * 0.001);
            if (score < bestScore) {
                bestScore = score;
                best = c;
            }
        }

        return best;
    }
}
