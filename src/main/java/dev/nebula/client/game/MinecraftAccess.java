package dev.nebula.client.game;

import dev.nebula.client.Main;
import dev.nebula.client.bridge.NativeBridge;
import dev.nebula.client.mappings.wrapper.client.Minecraft;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

public final class MinecraftAccess implements GameAccess {
    private final InventorySync sync = new InventorySync();

    public InventorySync inventorySync() {
        return sync;
    }

    public void inventoryPacket(Object packet, int kind) {
        inventoryCache = null;
        stackCache.clear();
        if (kind == 3) {
            velocityPacket(packet);
            return;
        }
        if (kind == 0)
            sync.acknowledge(
                    integer(map.call(packet, "S32PacketConfirmTransaction", "getWindowId")),
                    ((Number) map.call(packet, "S32PacketConfirmTransaction", "getActionNumber"))
                            .shortValue(),
                    Boolean.TRUE.equals(
                            map.call(packet, "S32PacketConfirmTransaction", "accepted")));
        else if (kind == 1) {
            int id = integer(map.call(packet, "S30PacketWindowItems", "getWindowId"));
            Object open = map.get(player(), "EntityPlayer", "openContainer");
            if (sync.active()
                    && id == integer(map.get(open, "Container", "windowId"))
                    && !sync.owns(open, id)) sync.replacementFull(now());
            else sync.full(id, now());
        }
    }

    private final MappedAccess map = new MappedAccess();
    private final Map<Object, Stack> stackCache = new IdentityHashMap<>(),
            visualStacks = new IdentityHashMap<>();
    private final Map<Integer, float[]> iconCache = new HashMap<>();
    private Inventory inventoryCache;
    private Object hiddenChestScreen, hiddenChestOwner, hiddenChestWorld;
    private int hiddenChestWindow;

    private void forgetHiddenChest() {
        hiddenChestScreen = hiddenChestOwner = hiddenChestWorld = null;
    }

    private boolean hiddenChestValid() {
        if (hiddenChestScreen == null || !inGame() || world() != hiddenChestWorld) return false;
        Object open = map.get(player(), "EntityPlayer", "openContainer");
        return open == hiddenChestOwner
                && integer(map.get(open, "Container", "windowId")) == hiddenChestWindow;
    }

    public void hideChest(boolean hidden) {
        if (!hidden) {
            if (hiddenChestValid() && screen() == null) {
                Object restore = hiddenChestScreen;
                forgetHiddenChest();
                map.call(mc(), "Minecraft", "displayGuiScreen", restore);
            } else forgetHiddenChest();
            return;
        }
        if (hiddenChestScreen != null || !inGame() || !map.is(screen(), "GuiChest")) return;
        Object open = map.get(player(), "EntityPlayer", "openContainer");
        if (!map.is(open, "ContainerChest")) return;
        hiddenChestScreen = screen();
        hiddenChestOwner = open;
        hiddenChestWorld = world();
        hiddenChestWindow = integer(map.get(open, "Container", "windowId"));
        map.set(mc(), "Minecraft", "currentScreen", null);
        map.set(mc(), "Minecraft", "inGameHasFocus", false);
        map.call(mc(), "Minecraft", "setIngameFocus");
        restoreHeldKeys();
    }

    private void restoreHeldKeys() {
        if (!org.lwjgl.input.Keyboard.isCreated()) return;
        Object options = map.get(mc(), "Minecraft", "gameSettings");
        for (String name :
                new String[] {
                    "keyBindForward",
                    "keyBindBack",
                    "keyBindLeft",
                    "keyBindRight",
                    "keyBindJump",
                    "keyBindSneak",
                    "keyBindSprint",
                    "keyBindAttack",
                    "keyBindUseItem"
                }) {
            Object binding = map.get(options, "GameSettings", name);
            int key = integer(map.get(binding, "KeyBinding", "keyCode"));
            boolean down =
                    key < 0
                            ? Mouse.isCreated()
                                    && -key - 100 >= 0
                                    && -key - 100 < Mouse.getButtonCount()
                                    && Mouse.isButtonDown(-key - 100)
                            : key > 0
                                    && key < org.lwjgl.input.Keyboard.KEYBOARD_SIZE
                                    && org.lwjgl.input.Keyboard.isKeyDown(key);
            map.set(binding, "KeyBinding", "pressed", down);
        }
    }

    private void checkHiddenChest() {
        if (hiddenChestScreen == null) return;
        if (!hiddenChestValid()) {
            forgetHiddenChest();
            return;
        }
        if (screen() != null) {
            Object packet = map.construct("C0DPacketCloseWindow", hiddenChestWindow);
            map.call(
                    map.call(mc(), "Minecraft", "getNetHandler"),
                    "NetHandlerPlayClient",
                    "addToSendQueue",
                    packet);
            map.set(
                    player(),
                    "EntityPlayer",
                    "openContainer",
                    map.get(player(), "EntityPlayer", "inventoryContainer"));
            forgetHiddenChest();
        }
    }

    private Object cachedWorld, menuScreen;
    private long atlasChecked, tabChecked;
    private int atlasId;
    private List<String[]> tabCache = Collections.emptyList();
    private static final java.util.regex.Pattern CLAN =
            java.util.regex.Pattern.compile("\\[([^\\]]+)\\]");

    public void prepareTick() {
        checkHiddenChest();
        stackCache.clear();
        visualStacks.clear();
        inventoryCache = null;
        if (world() != cachedWorld) {
            cachedWorld = world();
            sync.reset();
            iconCache.clear();
            tabCache = Collections.emptyList();
            tabChecked = 0;
        }
    }

    private Class<?> blankScreenType;

    private Object blankScreen() {
        try {
            if (blankScreenType == null) {
                Class<?> base = map.type("GuiScreen");
                String parent = org.objectweb.asm.Type.getInternalName(base);
                String name = "dev/nebula/client/game/BlankScreen";
                org.objectweb.asm.ClassWriter writer =
                        new org.objectweb.asm.ClassWriter(
                                org.objectweb.asm.ClassWriter.COMPUTE_MAXS);
                writer.visit(
                        org.objectweb.asm.Opcodes.V1_8,
                        org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_SUPER,
                        name,
                        null,
                        parent,
                        null);
                org.objectweb.asm.MethodVisitor init =
                        writer.visitMethod(
                                org.objectweb.asm.Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
                init.visitCode();
                init.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
                init.visitMethodInsn(
                        org.objectweb.asm.Opcodes.INVOKESPECIAL, parent, "<init>", "()V", false);
                init.visitInsn(org.objectweb.asm.Opcodes.RETURN);
                init.visitMaxs(0, 0);
                init.visitEnd();
                writer.visitEnd();
                byte[] bytes = writer.toByteArray();
                ClassLoader loader = base.getClassLoader();
                java.lang.reflect.Method define =
                        ClassLoader.class.getDeclaredMethod(
                                "defineClass", String.class, byte[].class, int.class, int.class);
                define.setAccessible(true);
                String dotted = name.replace('/', '.');
                try {
                    blankScreenType = Class.forName(dotted, false, loader);
                } catch (ClassNotFoundException absent) {
                    blankScreenType =
                            (Class<?>) define.invoke(loader, dotted, bytes, 0, bytes.length);
                }
            }
            return blankScreenType.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new MappedAccess.Missing("Blank screen unavailable", e);
        }
    }

    public void setMenuCapture(boolean captured) {
        if (mc() == null) return;
        if (captured) {
            if (menuScreen == null) {
                Object screen = blankScreen();
                map.call(mc(), "Minecraft", "displayGuiScreen", screen);
                menuScreen = screen;
            }
        } else {
            if (menuScreen != null && screen() == menuScreen)
                map.call(mc(), "Minecraft", "displayGuiScreen", new Object[] {null});
            menuScreen = null;
        }
    }

    public boolean menuCaptured() {
        return menuScreen != null && mc() != null && screen() == menuScreen;
    }

    public Object graphicsOption(String name) {
        try {
            return map.get(map.get(mc(), "Minecraft", "gameSettings"), "GameSettings", name);
        } catch (MappedAccess.Missing missing) {
            return null;
        }
    }

    public void graphicsOption(String name, Object value) {
        map.set(map.get(mc(), "Minecraft", "gameSettings"), "GameSettings", name, value);
    }

    public void refreshRenderer() {
        Object renderer = map.get(mc(), "Minecraft", "renderGlobal");
        if (renderer != null) map.call(renderer, "RenderGlobal", "loadRenderers");
    }

    public MappedAccess mappings() {
        return map;
    }

    private Object mc() {
        return Minecraft.getMinecraft().getRaw();
    }

    public Object player() {
        return mc() == null ? null : map.get(mc(), "Minecraft", "thePlayer");
    }

    public Object world() {
        return mc() == null ? null : map.get(mc(), "Minecraft", "theWorld");
    }

    public boolean inGame() {
        return player() != null && world() != null;
    }

    private Object screen() {
        return map.get(mc(), "Minecraft", "currentScreen");
    }

    public boolean guiOpen() {
        return NativeBridge.isGuiOpen() || mc() != null && screen() != null;
    }

    public boolean inventoryOpen() {
        return mc() != null && map.is(screen(), "GuiInventory");
    }

    public boolean inventoryReady() {
        if (!inGame()) return false;
        Object p = player();
        return map.get(p, "EntityPlayer", "openContainer")
                == map.get(p, "EntityPlayer", "inventoryContainer");
    }

    public boolean focused() {
        return (!Display.isCreated() || Display.isActive())
                && Boolean.TRUE.equals(map.get(mc(), "Minecraft", "inGameHasFocus"));
    }

    public boolean mouseHeld(int button) {
        if (Mouse.isCreated()) return Mouse.isButtonDown(button);
        String binding = button == 0 ? "keyBindAttack" : button == 1 ? "keyBindUseItem" : null;
        if (binding == null) return false;
        Object settings = map.get(mc(), "Minecraft", "gameSettings");
        return Boolean.TRUE.equals(
                map.get(map.get(settings, "GameSettings", binding), "KeyBinding", "pressed"));
    }

    public long now() {
        return System.nanoTime() / 1000000L;
    }

    private int integer(Object value) {
        return ((Number) value).intValue();
    }

    private double number(Object value) {
        return ((Number) value).doubleValue();
    }

    public Stack stack(Object raw) {
        return readStack(raw, true);
    }

    private Stack visualStack(Object raw) {
        return readStack(raw, false);
    }

    private Stack readStack(Object raw, boolean tags) {
        if (raw == null) return null;
        Map<Object, Stack> cache = tags ? stackCache : visualStacks;
        Stack cached = cache.get(raw);
        if (cached != null) return cached;
        Object item = map.call(raw, "ItemStack", "getItem");
        int type = -1, protection = 0;
        if (map.is(item, "ItemArmor")) {
            type = integer(map.get(item, "ItemArmor", "armorType"));
            protection = integer(map.get(item, "ItemArmor", "damageReduceAmount"));
        }
        Object tag = tags ? map.call(raw, "ItemStack", "getTagCompound") : null;
        Stack value =
                new Stack(
                        raw,
                        integer(map.call(null, "Item", "getIdFromItem", item)),
                        type,
                        protection,
                        integer(map.call(raw, "ItemStack", "getItemDamage")),
                        integer(map.call(raw, "ItemStack", "getMaxDamage")),
                        integer(map.get(raw, "ItemStack", "stackSize")),
                        String.valueOf(map.call(raw, "ItemStack", "getDisplayName")),
                        tag == null ? "" : tag.toString());
        cache.put(raw, value);
        return value;
    }

    public Inventory inventory() {
        if (inventoryCache != null) return inventoryCache;
        Object p = player(),
                container = map.get(p, "EntityPlayer", "inventoryContainer"),
                inv = map.get(p, "EntityPlayer", "inventory");
        List<Stack> slots = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            Object slot = map.call(container, "Container", "getSlot", i);
            slots.add(slot == null ? null : stack(map.call(slot, "Slot", "getStack")));
        }
        return inventoryCache =
                new Inventory(
                        world(),
                        p,
                        container,
                        integer(map.get(container, "Container", "windowId")),
                        integer(map.get(inv, "InventoryPlayer", "currentItem")),
                        stack(map.get(inv, "InventoryPlayer", "itemStack")),
                        slots);
    }

    public void clickSlot(int slot, int button, int mode) {
        inventoryCache = null;
        stackCache.clear();
        if (!inventoryReady())
            throw new IllegalStateException("Inventory click requires player container");
        Object p = player(),
                container = map.get(p, "EntityPlayer", "inventoryContainer"),
                open = map.get(p, "EntityPlayer", "openContainer");
        if (container != open) throw new IllegalStateException("Foreign inventory container");
        map.call(
                map.get(mc(), "Minecraft", "playerController"),
                "PlayerControllerMP",
                "windowClick",
                integer(map.get(container, "Container", "windowId")),
                slot,
                button,
                mode,
                p);
        sync.sent(
                integer(map.get(container, "Container", "windowId")),
                ((Number) map.get(container, "Container", "transactionID")).shortValue());
    }

    public void openInventory() {
        if (screen() != null && !inventoryOpen())
            throw new IllegalStateException("Another screen is open");
        Object state = null;
        for (Object value : map.type("C16PacketClientStatus_State").getEnumConstants())
            if (((Enum<?>) value).name().equals("OPEN_INVENTORY_ACHIEVEMENT")) state = value;
        if (state == null) throw new MappedAccess.Missing("Open inventory status unavailable");
        Object packet = map.construct("C16PacketClientStatus", state);
        map.call(
                map.call(mc(), "Minecraft", "getNetHandler"),
                "NetHandlerPlayClient",
                "addToSendQueue",
                packet);
        map.call(mc(), "Minecraft", "displayGuiScreen", map.construct("GuiInventory", player()));
    }

    public void closeInventory() {
        if (inventoryOpen()) map.call(player(), "EntityPlayerSP", "closeScreen");
    }

    public void selectHotbar(int slot) {
        inventoryCache = null;
        if (slot < 0 || slot > 8) throw new IllegalArgumentException("Hotbar slot");
        Object inv = map.get(player(), "EntityPlayer", "inventory");
        map.set(inv, "InventoryPlayer", "currentItem", slot);
        map.call(
                map.get(mc(), "Minecraft", "playerController"),
                "PlayerControllerMP",
                "updateController");
    }

    public void useKey(boolean pressed) {
        Object settings = map.get(mc(), "Minecraft", "gameSettings");
        map.set(
                map.get(settings, "GameSettings", "keyBindUseItem"),
                "KeyBinding",
                "pressed",
                pressed);
    }

    public boolean usingItem() {
        return Boolean.TRUE.equals(map.call(player(), "EntityPlayer", "isUsingItem"));
    }

    public void startUse() {
        Inventory inv = inventory();
        Stack held = inv.at(36 + inv.selected);
        if (held == null) throw new IllegalStateException("No held item");
        map.call(
                map.get(mc(), "Minecraft", "playerController"),
                "PlayerControllerMP",
                "sendUseItem",
                player(),
                world(),
                held.handle);
    }

    public void stopUse() {
        map.call(
                map.get(mc(), "Minecraft", "playerController"),
                "PlayerControllerMP",
                "onStoppedUsingItem",
                player());
    }

    public int potionTicks(int id) {
        Collection<?> effects =
                (Collection<?>) map.call(player(), "EntityLivingBase", "getActivePotionEffects");
        for (Object effect : effects)
            if (integer(map.call(effect, "PotionEffect", "getPotionID")) == id)
                return integer(map.call(effect, "PotionEffect", "getDuration"));
        return 0;
    }

    public MovementState movementState() {
        if (!inGame()) return null;
        Object p = player(), input = map.get(p, "EntityPlayerSP", "movementInput");
        if (input == null) return null;
        MovementState state = new MovementState();
        state.forward = ((Number) map.get(input, "MovementInput", "moveForward")).floatValue();
        state.strafe = ((Number) map.get(input, "MovementInput", "moveStrafe")).floatValue();
        state.yaw = ((Number) map.get(p, "Entity", "rotationYaw")).floatValue();
        state.onGround = Boolean.TRUE.equals(map.get(p, "Entity", "onGround"));
        state.sprinting = Boolean.TRUE.equals(map.call(p, "Entity", "isSprinting"));
        state.sneaking = Boolean.TRUE.equals(map.get(input, "MovementInput", "sneak"));
        state.collision = Boolean.TRUE.equals(map.get(p, "Entity", "isCollidedHorizontally"));
        state.using = usingItem();
        state.blind = potionTicks(15) > 0;
        state.water = Boolean.TRUE.equals(map.call(p, "Entity", "isInWater"));
        state.flyingAllowed =
                Boolean.TRUE.equals(
                        map.get(
                                map.get(p, "EntityPlayer", "capabilities"),
                                "PlayerCapabilities",
                                "allowFlying"));
        state.food =
                integer(
                        map.call(
                                map.call(p, "EntityPlayer", "getFoodStats"),
                                "FoodStats",
                                "getFoodLevel"));
        state.hurt = integer(map.get(p, "EntityLivingBase", "hurtTime"));
        state.entityId = integer(map.get(p, "Entity", "entityId"));
        return state;
    }

    private Object sprintBinding() {
        return map.get(map.get(mc(), "Minecraft", "gameSettings"), "GameSettings", "keyBindSprint");
    }

    public void sprintKey(boolean pressed) {
        map.set(sprintBinding(), "KeyBinding", "pressed", pressed);
    }

    public boolean physicalSprintKey() {
        int key = integer(map.get(sprintBinding(), "KeyBinding", "keyCode"));
        if (key < 0) return Mouse.isCreated() && -key - 100 >= 0 && Mouse.isButtonDown(-key - 100);
        return org.lwjgl.input.Keyboard.isCreated()
                && key > 0
                && key < org.lwjgl.input.Keyboard.KEYBOARD_SIZE
                && org.lwjgl.input.Keyboard.isKeyDown(key);
    }

    public void stopSprint() {
        map.call(player(), "EntityPlayerSP", "setSprinting", false);
    }

    public void jumpInput() {
        map.set(
                map.get(player(), "EntityPlayerSP", "movementInput"),
                "MovementInput",
                "jump",
                true);
    }

    public void velocityPacket(Object packet) {
        dev.nebula.client.modules.Module module = Main.getModuleManager().getModule("JumpReset");
        if (module instanceof dev.nebula.client.modules.movement.JumpReset && module.isEnabled()) {
            int id = integer(map.get(packet, "S12PacketEntityVelocity", "entityID"));
            int x = integer(map.get(packet, "S12PacketEntityVelocity", "motionX")),
                    z = integer(map.get(packet, "S12PacketEntityVelocity", "motionZ"));
            if (id == integer(map.get(player(), "Entity", "entityId")) && (x != 0 || z != 0))
                ((dev.nebula.client.modules.movement.JumpReset) module).velocity();
        }
    }

    public void clearHitDelay() {
        map.set(mc(), "Minecraft", "leftClickCounter", 0);
    }

    public void clearJumpDelay() {
        map.set(player(), "EntityLivingBase", "jumpTicks", 0);
    }

    public void attackClick() {
        map.call(mc(), "Minecraft", "clickMouse");
    }

    public void jitter() {
        Object p = player();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        float yaw = ((Number) map.get(p, "Entity", "rotationYaw")).floatValue(),
                pitch = ((Number) map.get(p, "Entity", "rotationPitch")).floatValue();
        map.set(p, "Entity", "rotationYaw", yaw + (float) random.nextDouble(-.15, .15));
        map.set(
                p,
                "Entity",
                "rotationPitch",
                Math.max(-90f, Math.min(90f, pitch + (float) random.nextDouble(-.15, .15))));
    }

    public static String clean(String text) {
        return text == null ? "" : text.replaceAll("\u00a7[0-9a-fk-orA-FK-OR]", "").trim();
    }

    private String team(Object p) {
        try {
            return readTeam(p);
        } catch (MappedAccess.Missing optional) {
            return "";
        }
    }

    private String readTeam(Object p) {
        Object team = map.call(p, "EntityPlayer", "getTeam");
        return team == null
                ? ""
                : String.valueOf(map.call(team, "ScorePlayerTeam", "getRegisteredName"));
    }

    private boolean enabled(String name) {
        dev.nebula.client.modules.Module module = Main.getModuleManager().getModule(name);
        return module != null && module.isEnabled();
    }

    private String clan(Object player) {
        try {
            return readClan(player);
        } catch (MappedAccess.Missing optional) {
            return "";
        }
    }

    private String readClan(Object player) {
        Object text = map.call(player, "Entity", "getDisplayName");
        String formatted =
                clean(String.valueOf(map.call(text, "IChatComponent", "getFormattedText")));
        java.util.regex.Matcher match = CLAN.matcher(formatted);
        return match.find() ? match.group(1).trim() : "";
    }

    public List<Actor> actors() {
        if (!inGame()) return Collections.emptyList();
        Object local = player();
        double lx = number(map.get(local, "Entity", "posX")),
                ly = number(map.get(local, "Entity", "posY")),
                lz = number(map.get(local, "Entity", "posZ"));
        String localClan = clan(local), localTeam = team(local);
        List<Actor> result = new ArrayList<>();
        boolean details = enabled("Nametags") || enabled("DamageIndicator");
        Set<String> allies = new HashSet<>(Main.getClanManager().getAlliedPlayers()),
                clans = new HashSet<>(Main.getClanManager().getAlliedClans());
        Collection<?> source =
                (Collection<?>)
                        map.get(
                                world(),
                                "WorldClient", "playerEntities");
        for (Object entity : source) {
            if (entity == null
                    || entity == local
                    || Boolean.TRUE.equals(map.get(entity, "Entity", "isDead"))) continue;
            boolean player = map.is(entity, "EntityPlayer");
            if (!player) continue;
            String kind =
                    player
                            ? "Players"
                            : map.is(entity, "EntityMob")
                                            ? "Mobs"
                                            : map.is(entity, "EntityAnimal")
                                                    ? "Animals"
                                                    : map.is(entity, "EntityArmorStand")
                                                            ? "ArmorStand"
                                                            : "Other";
            double x = number(map.get(entity, "Entity", "posX")),
                    y = number(map.get(entity, "Entity", "posY")),
                    z = number(map.get(entity, "Entity", "posZ"));
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) continue;
            boolean ally = false;
            String name = "", clan = "";
            List<Stack> armor = Collections.emptyList();
            if (player) {
                name = clean(String.valueOf(map.call(entity, "Entity", "getName")));
                clan = clan(entity);
                String otherTeam = team(entity);
                ally =
                        allies.contains(name)
                                || clans.contains(clan)
                                || !localTeam.isEmpty() && localTeam.equals(otherTeam)
                                || !localClan.isEmpty() && localClan.equalsIgnoreCase(clan);
                if (clan.isEmpty()) clan = otherTeam;
                if (details) {
                    armor = new ArrayList<>(4);
                    for (int type = 0; type < 4; type++)
                        armor.add(
                                visualStack(
                                        map.call(
                                                entity,
                                                "EntityPlayer",
                                                "getCurrentArmor",
                                                3 - type)));
                }
            }
            result.add(
                    new Actor(
                            entity,
                            integer(map.get(entity, "Entity", "entityId")),
                            name,
                            clan,
                            kind,
                            x,
                            y,
                            z,
                            number(map.get(entity, "Entity", "lastTickPosX")),
                            number(map.get(entity, "Entity", "lastTickPosY")),
                            number(map.get(entity, "Entity", "lastTickPosZ")),
                            number(map.get(entity, "Entity", "height")),
                            Math.sqrt(
                                    (x - lx) * (x - lx)
                                            + (y - ly) * (y - ly)
                                            + (z - lz) * (z - lz)),
                            player && details
                                    ? ((Number) map.call(entity, "EntityLivingBase", "getHealth"))
                                            .floatValue()
                                    : 0,
                            player && details
                                    ? ((Number)
                                                    map.call(
                                                            entity,
                                                            "EntityLivingBase",
                                                            "getMaxHealth"))
                                            .floatValue()
                                    : 0,
                            ally,
                            armor,
                            null));
        }
        return Collections.unmodifiableList(result);
    }

    public List<String[]> tabPlayers() {
        if (now() - tabChecked < 500) return tabCache;
        tabChecked = now();
        Object handler = map.call(mc(), "Minecraft", "getNetHandler");
        List<String[]> players = new ArrayList<>();
        for (Object info :
                new ArrayList<>(
                        ((Map<?, ?>) map.get(handler, "NetHandlerPlayClient", "playerInfoMap"))
                                .values())) {
            Object profile = map.get(info, "NetworkPlayerInfo", "gameProfile"),
                    team = map.call(info, "NetworkPlayerInfo", "getPlayerTeam");
            players.add(
                    new String[] {
                        String.valueOf(map.call(profile, "GameProfile", "getName")),
                        team == null
                                ? ""
                                : String.valueOf(
                                        map.call(team, "ScorePlayerTeam", "getRegisteredName"))
                    });
        }
        return tabCache = Collections.unmodifiableList(players);
    }

    public Object crosshairTarget() {
        Object hit = map.get(mc(), "Minecraft", "objectMouseOver");
        return hit == null ? null : map.get(hit, "MovingObjectPosition", "entityHit");
    }

    public float[] icon(Stack stack) {
        if (stack == null) return new float[5];
        if (now() - atlasChecked > 1000) {
            atlasChecked = now();
            Object textures = map.call(mc(), "Minecraft", "getTextureMapBlocks");
            int id = integer(map.call(textures, "ITextureObject", "getGlTextureId"));
            if (id != atlasId) {
                atlasId = id;
                iconCache.clear();
            }
        }
        float[] cached = iconCache.get(stack.itemId);
        if (cached != null) return cached;
        Object renderer = map.call(mc(), "Minecraft", "getRenderItem"),
                mesher = map.call(renderer, "RenderItem", "getItemModelMesher"),
                model = map.call(mesher, "ItemModelMesher", "getItemModel", stack.handle),
                sprite = map.call(model, "IBakedModel", "getParticleTexture");
        float[] value = {
            atlasId,
            ((Number) map.call(sprite, "TextureAtlasSprite", "getMinU")).floatValue(),
            ((Number) map.call(sprite, "TextureAtlasSprite", "getMinV")).floatValue(),
            ((Number) map.call(sprite, "TextureAtlasSprite", "getMaxU")).floatValue(),
            ((Number) map.call(sprite, "TextureAtlasSprite", "getMaxV")).floatValue()
        };
        if (iconCache.size() > 128) iconCache.clear();
        iconCache.put(stack.itemId, value);
        return value;
    }

    public float[] skin(Actor actor) {
        Object location = map.call(actor.handle, "AbstractClientPlayer", "getLocationSkin"),
                textures = map.call(mc(), "Minecraft", "getTextureManager"),
                texture = map.call(textures, "TextureManager", "getTexture", location);
        if (texture == null) return new float[5];
        return new float[] {
            ((Number) map.call(texture, "ITextureObject", "getGlTextureId")).floatValue(),
            .125f,
            .125f,
            .25f,
            .25f
        };
    }

    public Container chest() {
        if (mc() == null
                || !(map.is(screen(), "GuiChest") || screen() == null && hiddenChestValid()))
            return null;
        Object p = player(), container = map.get(p, "EntityPlayer", "openContainer");
        if (!map.is(container, "ContainerChest")) return null;
        Object lower = map.call(container, "ContainerChest", "getLowerChestInventory"),
                inv = map.get(p, "EntityPlayer", "inventory"),
                text = map.call(lower, "IWorldNameable", "getDisplayName");
        int size = integer(map.call(lower, "IInventory", "getSizeInventory"));
        if (size < 9 || size > 54) return null;
        List<Stack> slots = new ArrayList<>(size + 36);
        for (int i = 0; i < size + 36; i++)
            slots.add(
                    stack(
                            map.call(
                                    map.call(container, "Container", "getSlot", i),
                                    "Slot",
                                    "getStack")));
        return new Container(
                world(),
                container,
                integer(map.get(container, "Container", "windowId")),
                size,
                clean(String.valueOf(map.call(text, "IChatComponent", "getFormattedText"))),
                stack(map.get(inv, "InventoryPlayer", "itemStack")),
                slots);
    }

    public boolean ownsChestScreen(Object candidate, Object expectedContainer, int windowId) {
        if (candidate == null || candidate != screen() || !map.is(candidate, "GuiChest"))
            return false;
        Object container = map.get(player(), "EntityPlayer", "openContainer");
        return container == expectedContainer
                && integer(map.get(container, "Container", "windowId")) == windowId;
    }

    public void clickChest(Object expected, int windowId, int slot, int button, int mode) {
        inventoryCache = null;
        stackCache.clear();
        Object container = map.get(player(), "EntityPlayer", "openContainer");
        if (container != expected
                || integer(map.get(container, "Container", "windowId")) != windowId
                || !(map.is(screen(), "GuiChest") || screen() == null && hiddenChestValid()))
            throw new IllegalStateException("Chest changed before click");
        map.call(
                map.get(mc(), "Minecraft", "playerController"),
                "PlayerControllerMP",
                "windowClick",
                windowId,
                slot,
                button,
                mode,
                player());
        sync.sent(
                windowId, ((Number) map.get(container, "Container", "transactionID")).shortValue());
    }
}
