package dev.nebula.client.modules.combat.target;

import dev.nebula.client.mappings.wrapper.client.entity.EntityPlayerSP;
import dev.nebula.client.mappings.wrapper.client.multiplayer.WorldClient;
import dev.nebula.client.modules.combat.KillAura;
import dev.nebula.client.modules.combat.KillAura.Candidate;

import java.util.List;

public interface ITargetStrategy {

    TargetMode getMode();

    Candidate select(
            EntityPlayerSP localPlayer,
            WorldClient world,
            List<Candidate> candidates,
            TargetManager manager,
            KillAura aura
    );
}

