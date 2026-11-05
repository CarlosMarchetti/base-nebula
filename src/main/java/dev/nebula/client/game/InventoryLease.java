package dev.nebula.client.game;

public final class InventoryLease {
    private Object owner;
    private java.util.function.BooleanSupplier blocked = () -> false;

    public void barrier(java.util.function.BooleanSupplier blocked) {
        this.blocked = blocked;
    }

    public boolean acquire(Object candidate) {
        if (blocked.getAsBoolean()) return false;
        if (owner == null || owner == candidate) {
            owner = candidate;
            return true;
        }
        return false;
    }

    public void release(Object candidate) {
        if (owner == candidate) owner = null;
    }

    public boolean heldByOther(Object candidate) {
        return blocked.getAsBoolean() || owner != null && owner != candidate;
    }

    public void clear() {
        owner = null;
    }
}
