package dev.nebula.client.game;

import dev.nebula.client.game.GameAccess.Stack;
import java.util.*;

public final class InventorySync {
    public enum State {
        READY,
        WAITING,
        RESYNC
    }

    private final Set<Short> pending = new HashSet<>();
    private final Map<Integer, Stack> expected = new HashMap<>();
    private Object world, owner;
    private int window;
    private long deadline, resumeAt, confirmed, failures;
    private boolean active, rejected, sealed;
    private String reason = "";

    public void reset() {
        pending.clear();
        expected.clear();
        active = rejected = sealed = false;
        world = owner = null;
        deadline = resumeAt = 0;
        reason = "";
    }

    public void begin(Object world, Object owner, int window, long now) {
        if (active || now < resumeAt)
            throw new IllegalStateException("Inventario aguardando servidor");
        this.world = world;
        this.owner = owner;
        this.window = window;
        active = true;
        rejected = sealed = false;
        deadline = now + 3000;
        pending.clear();
        expected.clear();
        reason = "Aguardando confirmacao do servidor";
    }

    public boolean active() {
        return active;
    }

    public long confirmed() {
        return confirmed;
    }

    public long failures() {
        return failures;
    }

    public String reason() {
        return reason;
    }

    public void sent(int window, short action) {
        if (active && !sealed && this.window == window) pending.add(action);
    }

    public void expect(int slot, Stack stack) {
        if (active) expected.put(slot, stack);
    }

    public void seal() {
        if (!active) return;
        sealed = true;
        if (pending.isEmpty()) reject("Hook de confirmacao indisponivel");
    }

    public void reject(String reason) {
        if (active && !rejected) failures++;
        rejected = true;
        this.reason = reason;
    }

    public void acknowledge(int window, short action, boolean accepted) {
        if (!active || this.window != window || !pending.remove(action)) return;
        if (!accepted) reject("Troca rejeitada; aguardando inventario do servidor");
    }
    public void full(int window, long now) {
        if (!active || this.window != window) return;
        if (!rejected) reason = "Conferindo inventario recebido";
        pending.clear();
        sealed = true;
        if (rejected) {
            active = rejected = false;
            expected.clear();
            resumeAt = now + 500;
            reason = "Inventario ressincronizado";
        }
    }

    public void replacementFull(long now) {
        if (!active) return;
        reset();
        resumeAt = now + 500;
        reason = "Novo inventario recebido do servidor";
    }

    public boolean owns(Object handle, int id) {
        return active && owner == handle && window == id;
    }

    public State poll(GameAccess game) {
        long now = game.now();
        if (!active) return now < resumeAt ? State.WAITING : State.READY;
        if (game.world() != world) {
            reset();
            return State.READY;
        }
        GameAccess.Container chest = game.chest();
        GameAccess.Inventory inv = null;
        boolean sameChest = chest != null && chest.handle == owner && chest.windowId == window;
        if (!sameChest) inv = game.inventory();
        if (!sameChest
                && (inv.container != owner || inv.windowId != window || !game.inventoryReady())) {
            if (!rejected) reject("Janela fechada; reabra o inventario para ressincronizar");
        }
        if (!rejected && now >= deadline)
            reject("Tempo de confirmacao excedido; reabra o inventario");
        if (rejected) return State.RESYNC;
        if (!sealed || !pending.isEmpty()) return State.WAITING;
        Stack cursor = sameChest ? chest.cursor : inv.cursor;
        if (cursor != null) {
            reject("Cursor divergente; aguardando ressincronizacao");
            return State.RESYNC;
        }
        for (Map.Entry<Integer, Stack> entry : expected.entrySet()) {
            Stack actual = sameChest ? chest.at(entry.getKey()) : inv.at(entry.getKey());
            Stack wanted = entry.getValue();
            if (!(wanted == null
                    ? actual == null
                    : actual != null
                            && wanted.itemId == actual.itemId
                            && wanted.count == actual.count
                            && wanted.nbt.equals(actual.nbt)
                            && actual.damage >= wanted.damage
                            && actual.maxDamage == wanted.maxDamage)) {
                reject("Equipamento divergente; aguardando inventario do servidor");
                return State.RESYNC;
            }
        }
        active = false;
        expected.clear();
        confirmed++;
        reason = "Confirmado pelo servidor";
        return State.READY;
    }
}
