package dev.nebula.client.game;

import dev.nebula.client.game.GameAccess.Stack;
import java.util.*;

public final class InventoryTransaction {
    private InventoryTransaction() {}

    private static final class View {
        Object world, handle;
        int id;
        Stack cursor;
        List<Stack> slots;

        Stack at(int i) {
            return i < 0 || i >= slots.size() ? null : slots.get(i);
        }
    }

    private static View read(GameAccess game, boolean chest) {
        View view = new View();
        if (chest) {
            GameAccess.Container c = game.chest();
            if (c == null) throw new IllegalStateException("Bau fechado");
            view.world = c.world;
            view.handle = c.handle;
            view.id = c.windowId;
            view.cursor = c.cursor;
            view.slots = c.slots;
        } else {
            GameAccess.Inventory c = game.inventory();
            if (!game.inventoryReady()) throw new IllegalStateException("Inventario indisponivel");
            view.world = c.world;
            view.handle = c.container;
            view.id = c.windowId;
            view.cursor = c.cursor;
            view.slots = c.slots;
        }
        return view;
    }

    private static boolean same(Stack a, Stack b) {
        return a == null ? b == null : a.same(b);
    }

    private static void click(GameAccess game, boolean chest, Object owner, int id, int slot) {
        if (chest) game.clickChest(owner, id, slot, 0, 0);
        else game.clickSlot(slot, 0, 0);
    }

    private static void owner(View view, Object world, Object handle, int id) {
        if (view.world != world || view.handle != handle || view.id != id)
            throw new IllegalStateException("Container mudou durante transacao");
    }

    public static void exchange(
            GameAccess game,
            boolean chest,
            Object handle,
            int id,
            int source,
            int destination,
            Stack item,
            Stack old) {
        Object world = game.world();
        View before = read(game, chest);
        owner(before, world, handle, id);
        if (source == destination
                || item == null
                || before.cursor != null
                || !same(before.at(source), item)
                || !same(before.at(destination), old))
            throw new IllegalStateException("Slots alterados antes da troca");
        InventorySync sync = game.inventorySync();
        if (sync != null) sync.begin(world, handle, id, game.now());
        try {
            click(game, chest, handle, id, source);
            View picked = read(game, chest);
            owner(picked, world, handle, id);
            if (!same(picked.cursor, item) || picked.at(source) != null)
                throw new IllegalStateException("Retirada nao confirmada localmente");
            click(game, chest, handle, id, destination);
            View placed = read(game, chest);
            owner(placed, world, handle, id);
            if (!same(placed.at(destination), item) || !same(placed.cursor, old))
                throw new IllegalStateException("Destino nao confirmou a troca");
            if (old != null) click(game, chest, handle, id, source);
            View done = read(game, chest);
            owner(done, world, handle, id);
            if (done.cursor != null
                    || !same(done.at(destination), item)
                    || !same(done.at(source), old))
                throw new IllegalStateException("Cursor nao finalizou a troca");
            if (sync != null) {
                sync.expect(source, old);
                sync.expect(destination, item);
                sync.seal();
            }
        } catch (RuntimeException failure) {
            if (sync != null) sync.reject("Troca local incompleta; aguardando servidor");
            try {
                View view = read(game, chest);
                owner(view, world, handle, id);
                if (view.cursor != null
                        && (same(view.cursor, item) || same(view.cursor, old))
                        && view.at(source) == null) click(game, chest, handle, id, source);
            } catch (RuntimeException recovery) {
                failure.addSuppressed(recovery);
            }
            throw failure;
        }
    }

    public static void hotbar(
            GameAccess game, Object owner, int window, int source, int destination) {
        GameAccess.Inventory before = game.inventory();
        InventorySync sync = game.inventorySync();
        if (before.cursor != null || before.container != owner || before.windowId != window)
            throw new IllegalStateException("Inventario mudou antes da organizacao");
        if (sync != null) sync.begin(before.world, owner, window, game.now());
        try {
            game.clickSlot(source, destination - 36, 2);
            GameAccess.Inventory after = game.inventory();
            if (!same(before.at(source), after.at(destination))
                    || !same(before.at(destination), after.at(source)))
                throw new IllegalStateException("Organizacao divergente");
            if (sync != null) {
                sync.expect(source, before.at(destination));
                sync.expect(destination, before.at(source));
                sync.seal();
            }
        } catch (RuntimeException failure) {
            if (sync != null) sync.reject("Organizacao rejeitada; aguardando servidor");
            throw failure;
        }
    }

    public static void drop(GameAccess game, int slot, Stack expected) {
        GameAccess.Inventory inv = game.inventory();
        if (inv.cursor != null || !same(inv.at(slot), expected)) return;
        InventorySync sync = game.inventorySync();
        if (sync != null) sync.begin(inv.world, inv.container, inv.windowId, game.now());
        try {
            game.clickSlot(slot, 1, 4);
            if (sync != null) {
                sync.expect(slot, null);
                sync.seal();
            }
        } catch (RuntimeException failure) {
            if (sync != null) sync.reject("Descarte rejeitado; aguardando servidor");
            throw failure;
        }
    }
}
