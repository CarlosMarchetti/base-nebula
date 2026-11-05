import dev.nebula.client.Main;
import dev.nebula.client.features.Projection;
import dev.nebula.client.game.*;
import dev.nebula.client.game.GameAccess.Actor;
import dev.nebula.client.game.GameAccess.Inventory;
import dev.nebula.client.game.GameAccess.Stack;
import dev.nebula.client.gui.settings.SettingsManager;
import dev.nebula.client.modules.combat.*;
import dev.nebula.client.modules.movement.NoJumpDelay;
import dev.nebula.client.modules.visuals.*;
import dev.nebula.client.runtime.ClientContext;
import java.nio.file.*;
import java.util.*;

public class GameFeaturesCheck {
    public static class FakeGame implements GameAccess {
        public Object world = new Object(), player = new Object();
        public boolean gui, inv, active = true, held = true, using, key;
        public long time;
        public int selected = 4, potion, attacks, jumps, hits, jitters, opens, closes, stops;
        public Stack cursor;
        public List<Stack> slots = new ArrayList<>(Collections.nCopies(45, null));
        public List<String> clicks = new ArrayList<>();
        public List<Actor> entities = new ArrayList<>();
        public List<String[]> tab = new ArrayList<>();
        public int scans, reloads;
        public boolean captured;
        public Map<String, Object> graphics = new HashMap<>();
        public GameAccess.Container chest;

        public void prepareTick() {}

        public GameAccess.Container chest() {
            return chest;
        }

        public void clickChest(Object expected, int windowId, int slot, int button, int mode) {
            throw new UnsupportedOperationException("Specialized chest fixture required");
        }

        public void setMenuCapture(boolean value) {
            captured = value;
        }

        public boolean menuCaptured() {
            return captured;
        }

        public Object graphicsOption(String name) {
            return graphics.get(name);
        }

        public void graphicsOption(String name, Object value) {
            graphics.put(name, value);
        }

        public void refreshRenderer() {
            reloads++;
        }

        public boolean inGame() {
            return world != null && player != null;
        }

        public Object world() {
            return world;
        }

        public Object player() {
            return player;
        }

        public boolean guiOpen() {
            return gui || inv;
        }

        public boolean inventoryReady() {
            return (inv || !gui) && chest() == null;
        }

        public boolean inventoryOpen() {
            return inv;
        }

        public boolean focused() {
            return active;
        }

        public boolean mouseHeld(int b) {
            return held;
        }

        public long now() {
            return time;
        }

        public Inventory inventory() {
            return new Inventory(world, player, slots, 0, selected, cursor, slots);
        }

        public void clickSlot(int slot, int button, int mode) {
            if (!inventoryReady())
                throw new AssertionError("Clicked outside the player inventory container");
            clicks.add(slot + ":" + button + ":" + mode);
            if (mode == 4) slots.set(slot, null);
            else if (mode == 2) {
                Stack old = slots.get(slot);
                slots.set(slot, slots.get(36 + button));
                slots.set(36 + button, old);
            } else {
                Stack old = slots.get(slot);
                slots.set(slot, cursor);
                cursor = old;
            }
        }

        public void openInventory() {
            inv = true;
            opens++;
        }

        public void closeInventory() {
            inv = false;
            closes++;
        }

        public void selectHotbar(int slot) {
            selected = slot;
        }

        public void useKey(boolean value) {
            key = value;
        }

        public boolean usingItem() {
            return using;
        }

        public void startUse() {
            using = true;
        }

        public void stopUse() {
            using = false;
            stops++;
        }

        public int potionTicks(int id) {
            return potion;
        }

        public void clearHitDelay() {
            hits++;
        }

        public void clearJumpDelay() {
            jumps++;
        }

        public void attackClick() {
            attacks++;
        }

        public void jitter() {
            jitters++;
        }

        public List<Actor> actors() {
            scans++;
            return Collections.unmodifiableList(new ArrayList<>(entities));
        }

        public List<String[]> tabPlayers() {
            return tab;
        }

        public Object crosshairTarget() {
            return null;
        }

        public float[] icon(Stack s) {
            return new float[5];
        }

        public float[] skin(Actor a) {
            return new float[5];
        }
    }

    public static ClientContext install(FakeGame game, Path directory) throws Exception {
        ClientContext c =
                new ClientContext(
                        new SettingsManager(), directory.resolve("config.txt"), null, game);
        java.lang.reflect.Field f = Main.class.getDeclaredField("context");
        f.setAccessible(true);
        f.set(null, c);
        c.dispatcher().bindCurrentThread();
        c.modules().initializeModules();
        c.features().beginTick();
        return c;
    }

    public static void check(boolean b, String text) {
        if (!b) throw new AssertionError(text);
    }

}
