package dev.nebula.client.modules.movement;

import dev.nebula.client.game.GameAccess.MovementState;
import dev.nebula.client.modules.*;

public final class JumpReset extends GameModule {
    private long velocityUntil;
    private int previousHurt, cooldown;
    private boolean armed;

    public JumpReset() {
        super("JumpReset", "Pulo contextual ao receber dano com knockback", Category.MOVEMENT);
    }

    public void setup() {
        bool("Require Sprint", true);
        integer("Cooldown Ticks", 2, 0, 20);
        keybind();
    }

    public void velocity() {
        velocityUntil = game().now() + 400;
        armed = true;
    }

    protected void tick() {}

    public void input(MovementState state) {
        if (cooldown > 0) cooldown--;
        if (state == null || game().guiOpen() || !game().focused()) {
            previousHurt = state == null ? 0 : state.hurt;
            return;
        }
        boolean fresh = state.hurt >= 8;
        if (fresh
                && armed
                && game().now() <= velocityUntil
                && state.onGround
                && !state.water
                && !state.sneaking
                && (!getValueBoolean("Require Sprint") || state.sprinting)
                && cooldown == 0) {
            game().jumpInput();
            armed = false;
            cooldown = count("Cooldown Ticks");
        }
        if (game().now() > velocityUntil) armed = false;
        previousHurt = state.hurt;
    }

    public void onDisable() {
        armed = false;
        previousHurt = cooldown = 0;
        super.onDisable();
    }

    protected void worldLost() {
        armed = false;
        previousHurt = cooldown = 0;
        super.worldLost();
    }
}
