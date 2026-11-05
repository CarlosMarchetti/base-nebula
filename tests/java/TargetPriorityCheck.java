import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.mappings.wrapper.entity.Entity;
import dev.nebula.client.modules.combat.KillAura.Candidate;
import dev.nebula.client.modules.combat.target.*;
import dev.nebula.client.modules.combat.target.strategies.*;
import java.util.*;

public class TargetPriorityCheck {
    public static class Armor {
        int damage;

        Armor(int d) {
            damage = d;
        }

        public int func_77958_k() {
            return 500;
        }

        public int func_77960_j() {
            return damage;
        }
    }

    public static class Player {
        public boolean isDead;
        String name;
        Armor[] armor = new Armor[4];

        Player(String n, int damage) {
            name = n;
            for (int i = 0; i < 4; i++) armor[i] = new Armor(damage);
        }

        public String getName() {
            return name;
        }

        public Armor func_82169_q(int slot) {
            return armor[slot];
        }
    }

    static Candidate candidate(Player p, double distance, float health) {
        return new Candidate(new Entity(p), new double[6], distance, 0, health);
    }

    static void check(boolean b, String s) {
        if (!b) throw new AssertionError(s);
    }

    public static void main(String[] args) throws Exception {
        Mappings.ENTITY_PLAYER_GET_NAME = Player.class.getMethod("getName");
        Player weak = new Player("weak", 490),
                fresh = new Player("fresh", 0),
                partial = new Player("partial", 0);
        partial.armor[0] = partial.armor[1] = partial.armor[2] = null;
        Candidate w = candidate(weak, 3, 15),
                f = candidate(fresh, 1, 5),
                p = candidate(partial, 2, 20);
        TargetManager manager = new TargetManager();
        check(
                new LowArmorStrategy().select(null, null, Arrays.asList(w, f, p), manager, null)
                        == p,
                "Low Armor did not prioritize missing equipment");
        BreakArmorStrategy strategy = new BreakArmorStrategy();
        check(
                strategy.select(null, null, Arrays.asList(f, w), manager, null) == w,
                "Break Armor preferred fresh equipment");
        fresh.armor[0].damage = 499;
        check(
                strategy.select(null, null, Arrays.asList(f, w), manager, null) == w,
                "Break Armor lost an intact locked target");
        weak.armor[0] = new Armor(0);
        check(
                strategy.select(null, null, Arrays.asList(f, w), manager, null) == f,
                "Armor replacement did not reevaluate priority");
        check(
                new HealthStrategy().select(null, null, Arrays.asList(w, f), manager, null) == f,
                "Health did not choose lowest valid health");
        check(TargetMode.values().length == 3, "Obsolete modes remained registered");
        System.out.println(
                "PASS three priorities, SRG armor reads, weak-piece focus, stable lock and replacement re-evaluation");
    }
}
