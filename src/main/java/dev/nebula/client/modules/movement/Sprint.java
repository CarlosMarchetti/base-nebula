package dev.nebula.client.modules.movement;

import dev.nebula.client.game.GameAccess.MovementState;
import dev.nebula.client.modules.*;
import dev.nebula.client.utils.RotationUtils;

public final class Sprint extends GameModule {
    private boolean owned;

    public Sprint() {
        super(
                "Sprint",
                "Sprint automatico com as condicoes vanilla e entrada atualizada",
                Category.MOVEMENT);
        setKey(46);
    }

    public void setup() {
        keybind();
    }

    protected void tick() {} 

    public void input(MovementState state) {
        boolean eligible =
                state != null
                        && !game().guiOpen()
                        && game().focused()
                        && state.forward >= .8f
                        && !state.sneaking
                        && !state.collision
                        && !state.using
                        && !state.blind
                        && (state.food > 6 || state.flyingAllowed);
        if (eligible && RotationUtils.getTargetRotation() != null) {
            double delta = Math.toRadians(state.yaw - RotationUtils.getTargetRotation().getYaw());
            eligible = state.forward * Math.cos(delta) + state.strafe * Math.sin(delta) > .0;
        }
        game().sprintKey(eligible);
        if (!eligible && owned && state != null && state.sprinting) game().stopSprint();
        owned = eligible;
    }

    public void onDisable() {
        try {
            if (game().inGame()) game().sprintKey(game().physicalSprintKey());
        } finally {
            owned = false;
            super.onDisable();
        }
    }

    protected void worldLost() {
        owned = false;
        super.worldLost();
    }
}
