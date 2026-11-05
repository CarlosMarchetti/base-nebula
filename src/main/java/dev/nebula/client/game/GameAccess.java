package dev.nebula.client.game;

import java.util.*;

public interface GameAccess {
    final class Stack {
        public final Object handle;
        public final int itemId, armorType, protection, damage, maxDamage, count;
        public final String name, nbt;

        public Stack(
                Object handle,
                int id,
                int type,
                int protection,
                int damage,
                int max,
                int count,
                String name) {
            this(handle, id, type, protection, damage, max, count, name, "");
        }

        public Stack(
                Object handle,
                int id,
                int type,
                int protection,
                int damage,
                int max,
                int count,
                String name,
                String nbt) {
            this.nbt = nbt;
            this.handle = handle;
            itemId = id;
            armorType = type;
            this.protection = protection;
            this.damage = damage;
            maxDamage = max;
            this.count = count;
            this.name = name;
        }

        public boolean same(Stack other) {
            return other != null
                    && itemId == other.itemId
                    && damage == other.damage
                    && maxDamage == other.maxDamage
                    && count == other.count
                    && nbt.equals(other.nbt);
        }

        public int durability() {
            return maxDamage <= 0
                    ? 100
                    : (int)
                            Math.max(
                                    0,
                                    Math.min(
                                            100,
                                            Math.floor(
                                                    100.0
                                                            / maxDamage)));
        }

        public int remaining() {
            return Math.max(0, maxDamage - damage);
        }

        public boolean usableArmor() {
            return armorType >= 0
                    && armorType < 4
                    && count == 1
                    && maxDamage > 0
                    && damage >= 0
                    && damage < maxDamage;
        }

        public long armorValue() {
            return armorType < 0 ? -1 : protection * 1000L + durability();
        }
    }

    final class Inventory {
        public final Object world, player, container;
        public final int windowId, selected;
        public final Stack cursor;
        public final List<Stack> slots;

        public Inventory(
                Object world,
                Object player,
                Object container,
                int id,
                int selected,
                Stack cursor,
                List<Stack> slots) {
            this.world = world;
            this.player = player;
            this.container = container;
            windowId = id;
            this.selected = selected;
            this.cursor = cursor;
            this.slots = Collections.unmodifiableList(new ArrayList<>(slots));
        }

        public Stack at(int slot) {
            return slot < 0 || slot >= slots.size() ? null : slots.get(slot);
        }
    }

    final class Actor {
        public final Object handle;
        public final int id;
        public final String name, clan, kind;
        public final double x, y, z, px, py, pz, height, distance;
        public final float health, maxHealth;
        public final boolean ally;
        public final List<Stack> armor;
        public final Stack item;

        public Actor(
                Object handle,
                int id,
                String name,
                String clan,
                String kind,
                double x,
                double y,
                double z,
                double px,
                double py,
                double pz,
                double height,
                double distance,
                float hp,
                float max,
                boolean ally,
                List<Stack> armor,
                Stack item) {
            this.handle = handle;
            this.id = id;
            this.name = name;
            this.clan = clan;
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.z = z;
            this.px = px;
            this.py = py;
            this.pz = pz;
            this.height = height;
            this.distance = distance;
            health = hp;
            maxHealth = max;
            this.ally = ally;
            this.armor = Collections.unmodifiableList(new ArrayList<>(armor));
            this.item = item;
        }
    }

    final class Container {
        public final Object world, handle;
        public final int windowId, chestSize;
        public final String title;
        public final Stack cursor;
        public final List<Stack> slots;

        public Container(
                Object world,
                Object handle,
                int id,
                int size,
                String title,
                Stack cursor,
                List<Stack> slots) {
            this.world = world;
            this.handle = handle;
            windowId = id;
            chestSize = size;
            this.title = title;
            this.cursor = cursor;
            this.slots = Collections.unmodifiableList(new ArrayList<>(slots));
        }

        public Stack at(int slot) {
            return slot < 0 || slot >= slots.size() ? null : slots.get(slot);
        }
    }

    final class MovementState {
        public float forward, strafe, yaw;
        public boolean onGround, sprinting, sneaking, collision, using, blind, flyingAllowed, water;
        public int food = 20, hurt, entityId;
    }

    default MovementState movementState() {
        return null;
    }

    default void sprintKey(boolean pressed) {}

    default boolean physicalSprintKey() {
        return false;
    }

    default void stopSprint() {}

    default void jumpInput() {}

    default void hideChest(boolean hidden) {}

    default void inventoryPacket(Object packet, int kind) {}

    default InventorySync inventorySync() {
        return null;
    }

    void prepareTick();

    Container chest();

    default boolean ownsChestScreen(Object screen, Object container, int windowId) {
        return false;
    }

    void clickChest(Object expected, int windowId, int slot, int button, int mode);

    void setMenuCapture(boolean captured);

    boolean menuCaptured();

    Object graphicsOption(String name);

    void graphicsOption(String name, Object value);

    void refreshRenderer();

    boolean inGame();

    Object world();

    Object player();

    boolean guiOpen();

    boolean inventoryOpen();

    default boolean inventoryReady() {
        return inventoryOpen();
    }

    boolean focused();

    boolean mouseHeld(int button);

    long now();

    Inventory inventory();

    void clickSlot(int slot, int button, int mode);

    void openInventory();

    void closeInventory();

    void selectHotbar(int slot);

    void useKey(boolean pressed);

    boolean usingItem();

    void startUse();

    void stopUse();

    int potionTicks(int id);

    void clearHitDelay();

    void clearJumpDelay();

    void attackClick();

    void jitter();

    List<Actor> actors();

    List<String[]> tabPlayers();

    Object crosshairTarget();

    float[] icon(Stack stack);

    float[] skin(Actor actor);
}
