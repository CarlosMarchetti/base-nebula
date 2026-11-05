package dev.nebula.client.mappings.wrapper.client;

import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.mappings.wrapper.client.entity.EntityPlayerSP;
import dev.nebula.client.mappings.wrapper.client.multiplayer.PlayerControllerMP;
import dev.nebula.client.mappings.wrapper.client.multiplayer.WorldClient;
import dev.nebula.client.mappings.wrapper.client.network.NetHandlerPlayClient;
import dev.nebula.client.mappings.wrapper.network.NetworkManager;
import dev.nebula.client.utils.Log;
import dev.nebula.client.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class Minecraft {

    private static Minecraft currentInstance;
    private final Object rawInstance;

    private Field cachedPlayerField = null;
    private Field cachedWorldField = null;
    private Method cachedNetHandlerMethod = null;
    private Field cachedNetHandlerField = null;

    private Minecraft(Object raw) {
        this.rawInstance = raw;
    }

    public static Minecraft getMinecraft() {
        if (currentInstance == null || currentInstance.rawInstance == null) {
            Object raw = resolveMinecraftInstance();
            currentInstance = new Minecraft(raw);
        }
        return currentInstance;
    }

    private static Object resolveMinecraftInstance() {
        try {
            if (Mappings.MINECRAFT_INSTANCE != null) {
                Object raw = ReflectionUtils.getFieldValue((Field) Mappings.MINECRAFT_INSTANCE, null);
                if (raw != null) return raw;
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> mcCls = ReflectionUtils.getClass("net.minecraft.client.Minecraft");
            for (Method m : mcCls.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers()) && m.getParameterTypes().length == 0 && m.getReturnType().equals(mcCls)) {
                    m.setAccessible(true);
                    Object raw = m.invoke(null);
                    if (raw != null) return raw;
                }
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> mcCls = ReflectionUtils.getClass("net.minecraft.client.Minecraft");
            for (Field f : mcCls.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) && f.getType().equals(mcCls)) {
                    f.setAccessible(true);
                    Object raw = f.get(null);
                    if (raw != null) return raw;
                }
            }
        } catch (Throwable ignored) {}

        return null;
    }

    public static void invalidate() {
        currentInstance = null;
    }

    public Object getRaw() {
        return rawInstance;
    }


    public EntityPlayerSP getThePlayer() {
        if (rawInstance == null) return null;

        try {
            if (Mappings.MINECRAFT_THE_PLAYER != null) {
                Object raw = ReflectionUtils.getFieldValue((Field) Mappings.MINECRAFT_THE_PLAYER, rawInstance);
                if (raw != null) return new EntityPlayerSP(raw);
            }
        } catch (Throwable ignored) {}

        if (cachedPlayerField != null) {
            try {
                Object raw = cachedPlayerField.get(rawInstance);
                if (raw != null) return new EntityPlayerSP(raw);
            } catch (Throwable ignored) {}
        }

        Class<?> cls = rawInstance.getClass();
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                String typeName = f.getType().getName();
                if (typeName.contains("EntityPlayer") || f.getName().equalsIgnoreCase("thePlayer")) {
                    try {
                        f.setAccessible(true);
                        Object val = f.get(rawInstance);
                        if (val != null) {
                            cachedPlayerField = f;
                            return new EntityPlayerSP(val);
                        }
                    } catch (Throwable ignored) {}
                }
            }
            cls = cls.getSuperclass();
        }

        return null;
    }

    public WorldClient getTheWorld() {
        if (rawInstance == null) return null;

        try {
            if (Mappings.MINECRAFT_THE_WORLD != null) {
                Object raw = ReflectionUtils.getFieldValue((Field) Mappings.MINECRAFT_THE_WORLD, rawInstance);
                if (raw != null) return new WorldClient(raw);
            }
        } catch (Throwable ignored) {}

        if (cachedWorldField != null) {
            try {
                Object raw = cachedWorldField.get(rawInstance);
                if (raw != null) return new WorldClient(raw);
            } catch (Throwable ignored) {}
        }

        Class<?> cls = rawInstance.getClass();
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                String typeName = f.getType().getName();
                if (typeName.contains("WorldClient") || typeName.contains("World") || f.getName().equalsIgnoreCase("theWorld")) {
                    try {
                        f.setAccessible(true);
                        Object val = f.get(rawInstance);
                        if (val != null && !f.getType().isPrimitive()) {
                            cachedWorldField = f;
                            return new WorldClient(val);
                        }
                    } catch (Throwable ignored) {}
                }
            }
            cls = cls.getSuperclass();
        }

        return null;
    }

    public PlayerControllerMP getPlayerController() {
        if (rawInstance == null) return null;
        try {
            if (Mappings.MINECRAFT_GET_PLAYER_CONTROLLER != null) {
                Object raw = ReflectionUtils.getFieldValue((Field) Mappings.MINECRAFT_GET_PLAYER_CONTROLLER, rawInstance);
                if (raw != null) return new PlayerControllerMP(raw);
            }
        } catch (Throwable ignored) {}

        for (Field f : rawInstance.getClass().getDeclaredFields()) {
            if (f.getType().getName().contains("PlayerController")) {
                try {
                    f.setAccessible(true);
                    Object val = f.get(rawInstance);
                    if (val != null) return new PlayerControllerMP(val);
                } catch (Throwable ignored) {}
            }
        }

        return new PlayerControllerMP(null);
    }

    public NetHandlerPlayClient getNetHandler() {
        if (rawInstance == null) return null;

        try {
            if (Mappings.MINECRAFT_NET_HANDLER != null) {
                Object raw = ReflectionUtils.invokeMethod((Method) Mappings.MINECRAFT_NET_HANDLER, rawInstance);
                if (raw != null) return new NetHandlerPlayClient(raw);
            }
        } catch (Throwable ignored) {}

        if (cachedNetHandlerMethod != null) {
            try {
                Object raw = cachedNetHandlerMethod.invoke(rawInstance);
                if (raw != null) return new NetHandlerPlayClient(raw);
            } catch (Throwable ignored) {}
        }

        if (cachedNetHandlerField != null) {
            try {
                Object raw = cachedNetHandlerField.get(rawInstance);
                if (raw != null) return new NetHandlerPlayClient(raw);
            } catch (Throwable ignored) {}
        }

        for (Method m : rawInstance.getClass().getMethods()) {
            if (m.getParameterTypes().length == 0 && m.getReturnType().getName().contains("NetHandlerPlayClient")) {
                try {
                    m.setAccessible(true);
                    Object val = m.invoke(rawInstance);
                    if (val != null) {
                        cachedNetHandlerMethod = m;
                        return new NetHandlerPlayClient(val);
                    }
                } catch (Throwable ignored) {}
            }
        }

        for (Field f : rawInstance.getClass().getDeclaredFields()) {
            if (f.getType().getName().contains("NetHandlerPlayClient")) {
                try {
                    f.setAccessible(true);
                    Object val = f.get(rawInstance);
                    if (val != null) {
                        cachedNetHandlerField = f;
                        return new NetHandlerPlayClient(val);
                    }
                } catch (Throwable ignored) {}
            }
        }

        return null;
    }

    public NetworkManager getNetworkManager() {
        try {
            NetHandlerPlayClient handler = getNetHandler();
            if (handler != null) {
                return handler.getNetworkManager();
            }
        } catch (Exception ignored) {}
        return null;
    }

    public boolean isInGui() {
        try {
            Object screen = ReflectionUtils.getFieldValue((Field) Mappings.MINECRAFT_CURRENT_SCREEN, rawInstance);
            return screen != null;
        } catch (Exception e) {
            return false;
        }
    }

    public int getDisplayWidth() {
        try {
            return ReflectionUtils.getInt((Field) Mappings.MINECRAFT_DISPLAY_WIDTH, rawInstance);
        } catch (Exception e) {
            return 854;
        }
    }

    public int getDisplayHeight() {
        try {
            return ReflectionUtils.getInt((Field) Mappings.MINECRAFT_DISPLAY_HEIGHT, rawInstance);
        } catch (Exception e) {
            return 480;
        }
    }

    public EntityPlayerSP thePlayer() { return getThePlayer(); }
    public WorldClient theWorld() { return getTheWorld(); }

    public static EntityPlayerSP getPlayer() { return getMinecraft().getThePlayer(); }
    public static WorldClient getWorld() { return getMinecraft().getTheWorld(); }
    public static PlayerControllerMP getPlayerControllerMP() { return getMinecraft().getPlayerController(); }
    public static NetHandlerPlayClient getNetHandlerPlayClient() { return getMinecraft().getNetHandler(); }
    public static NetworkManager getNetworkManagerStatic() { return getMinecraft().getNetworkManager(); }
    public static boolean inGameHasFocus() { return !getMinecraft().isInGui(); }
}
