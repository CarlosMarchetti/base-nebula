package dev.nebula.client.modules.combat;

import dev.nebula.client.game.GameAccess.*;
import dev.nebula.client.modules.*;

public final class ArmorFilter extends GameModule {
    private long next;

    public ArmorFilter() {
        super("ArmorFilter", "Descarta armaduras gastas com o inventario aberto", Category.COMBAT);
    }

    public void setup() {
        integer("Delay", 40, 10, 500);
        integer("Durability", 50, 0, 100);
        keybind();
    }

    protected void tick() {
        if (!game().inventoryOpen() || game().now() < next || !lease().acquire(this)) return;
        try {
            Inventory inv = game().inventory();
            if (inv.cursor != null) return;
            for (int slot = 9; slot <= 44; slot++) {
                Stack item = inv.at(slot);
                if (item != null
                        && item.armorType >= 0
                        && item.maxDamage > 0
                        && item.durability() <= count("Durability")) {
                    game().clickSlot(slot, 1, 4);
                    next = game().now() + count("Delay");
                    break;
                }
            }
        } finally {
            lease().release(this);
        }
    }

    protected void worldLost() {
        next = 0;
        super.worldLost();
    }
}
