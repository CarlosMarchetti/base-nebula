package dev.nebula.client.modules.combat;

import dev.nebula.client.modules.*;
import java.util.concurrent.ThreadLocalRandom;

public final class AutoClicker extends GameModule {
    private long deadline;
    private boolean armed;

    public AutoClicker() {
        super("AutoClicker", "Cliques vanilla com intervalo e CPS configuraveis", Category.COMBAT);
    }

    public void setup() {
        integer("Min CPS", 10, 1, 20);
        integer("Max CPS", 14, 1, 20);
        bool("Jitter", false);
        bool("Only While Holding", true);
        keybind();
    }

    public static long interval(int min, int max, double sample) {
        min = Math.max(1, min);
        max = Math.max(min, max);
        return Math.max(
                1, Math.round(1000.0 / (min + (max - min) * Math.max(0, Math.min(1, sample)))));
    }

    protected void tick() {
        if (game().guiOpen()
                || !game().focused()
                || lease().heldByOther(this)
                || getValueBoolean("Only While Holding") && !game().mouseHeld(0)) {
            armed = false;
            return;
        }
        long now = game().now();
        if (!armed) {
            armed = true;
            deadline = now + (game().mouseHeld(0) ? nextInterval() : 0);
        }
        if (now < deadline) return;
        game().clearHitDelay();
        if (getValueBoolean("Jitter")) game().jitter();
        game().attackClick();
        deadline = now + nextInterval();
    }

    private long nextInterval() {
        return interval(
                count("Min CPS"), count("Max CPS"), ThreadLocalRandom.current().nextDouble());
    }

    public void onDisable() {
        armed = false;
        deadline = 0;
        super.onDisable();
    }

    protected void worldLost() {
        armed = false;
        deadline = 0;
        super.worldLost();
    }
}
