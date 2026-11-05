import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.mappings.wrapper.client.Minecraft;
import dev.nebula.client.mappings.wrapper.entity.Entity;
import dev.nebula.client.modules.combat.KillAura;
import dev.nebula.client.modules.combat.target.TargetManager;

public class HitboxOwnershipCheck {
    static void check(boolean b, String text) {
        if (!b) throw new AssertionError(text);
    }

    public static void main(String[] args) throws Exception {
        CombatTickCheck.main(new String[0]);
        CombatTickCheck.Game game = CombatTickCheck.Game.instance;
        Mappings.ENTITY_LIST_FIELD = CombatTickCheck.World.class.getField("loadedEntityList");
        CombatTickCheck.Living untouched = new CombatTickCheck.Living();
        untouched.id = 100;
        untouched.width = .35f;
        untouched.height = 1.42f;
        game.world.loadedEntityList.add(untouched);
        TargetManager manager = new TargetManager();
        manager.manageHitboxes(Minecraft.getPlayer(), Minecraft.getWorld(), null, false);
        check(
                untouched.width == .35f && untouched.height == 1.42f,
                "Disabled zeroing overwrote vanilla dimensions");
        CombatTickCheck.Living chosen = game.world.loadedEntityList.get(1);
        KillAura.Candidate target =
                new KillAura.Candidate(new Entity(chosen), new double[6], 1, 0, 20);
        manager.manageHitboxes(Minecraft.getPlayer(), Minecraft.getWorld(), target, true);
        check(untouched.width == 0 && untouched.height == 0, "Zeroing fixture not applied");
        CombatTickCheck.Living reused = new CombatTickCheck.Living();
        reused.id = 100;
        reused.width = .25f;
        reused.height = .8f;
        game.world.loadedEntityList.remove(untouched);
        game.world.loadedEntityList.add(reused);
        manager.restoreAllHitboxes(Minecraft.getWorld());
        check(
                untouched.width == .35f
                        && untouched.height == 1.42f
                        && reused.width == .25f
                        && reused.height == .8f,
                "Restore touched an unowned entity with reused id");
        reused.height = .9f;
        manager.restoreAllHitboxes(Minecraft.getWorld());
        check(reused.height == .9f, "Repeated cleanup fabricated default dimensions");
        System.out.println(
                "PASS no idle hitbox writes, original dimensions, detached-entity restoration and reused entity ids");
    }
}
