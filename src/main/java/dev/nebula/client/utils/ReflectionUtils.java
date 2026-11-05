package dev.nebula.client.utils;

import dev.nebula.client.injection.Resources;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ReflectionUtils {

    private static final String MINECRAFT_CLASS = "net.minecraft.client.Minecraft";

    private static volatile ClassLoader gameLoader;

    private ReflectionUtils() {}


    public static void initClassLoader() throws Exception {
        if (gameLoader != null) return;

        synchronized (ReflectionUtils.class) {
            if (gameLoader != null) return;

            ClassLoader own = ReflectionUtils.class.getClassLoader();
            if (canLoadMinecraft(own)) {
                gameLoader = own;
                Log.info("ClassLoader do jogo: " + own.getClass().getName());
                return;
            }

            for (Thread thread : Thread.getAllStackTraces().keySet()) {
                ClassLoader candidate = thread.getContextClassLoader();
                if (canLoadMinecraft(candidate)) {
                    gameLoader = candidate;
                    Log.info("ClassLoader do jogo (via thread " + thread.getName() + "): " + candidate.getClass().getName());
                    return;
                }
            }
            throw new IllegalStateException("Nenhum classloader capaz de carregar " + MINECRAFT_CLASS);
        }
    }

    private static boolean canLoadMinecraft(ClassLoader loader) {
        if (loader == null) return false;
        try {
            Class.forName(MINECRAFT_CLASS, false, loader);
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    public static ClassLoader getGameLoader() throws Exception {
        if (gameLoader == null) initClassLoader();
        return gameLoader;
    }


    public static Class<?> getClass(String name) throws Exception {
        return getGameLoader().loadClass(name);
    }

    public static byte[] getClassBytes(Class<?> clazz) throws Exception {
        String path = clazz.getName().replace('.', '/') + ".class";

        try (InputStream in = openResource(path)) {
            if (in == null) {
                throw new IOException("Cannot read class bytes: " + path);
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[4096];
            int read;
            while ((read = in.read(data)) != -1) {
                buffer.write(data, 0, read);
            }
            return buffer.toByteArray();
        }
    }

    private static InputStream openResource(String path) throws Exception {
        InputStream in = getGameLoader().getResourceAsStream(path);
        return in != null ? in : ClassLoader.getSystemResourceAsStream(path);
    }


    public static Field getField(Class<?> clazz, String name) throws NoSuchFieldException {
        for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        Log.debug("Field not found: " + name + " in " + clazz.getName());
        throw new NoSuchFieldException(name);
    }

    public static Object getFieldValue(Field f, Object instance) throws Exception {
        if (f == null) return null;
        f.setAccessible(true);
        return f.get(instance);
    }

    public static void setFieldValue(Object instance, Field f, Object value) throws Exception {
        if (f == null) return;
        f.setAccessible(true);
        f.set(instance, value);
    }

    public static double getDouble(Field f, Object obj) throws Exception {
        if (f == null) return 0.0;
        f.setAccessible(true);
        return f.getDouble(obj);
    }

    public static int getInt(Field f, Object obj) throws Exception {
        if (f == null) return 0;
        f.setAccessible(true);
        return f.getInt(obj);
    }

    public static float getFloat(Field f, Object obj) throws Exception {
        if (f == null) return 0.0f;
        f.setAccessible(true);
        return f.getFloat(obj);
    }

    public static boolean getBoolean(Field f, Object obj) throws Exception {
        if (f == null) return false;
        f.setAccessible(true);
        return f.getBoolean(obj);
    }

    public static void setFloat(Field f, Object obj, float v) throws Exception {
        if (f == null) return;
        f.setAccessible(true);
        f.setFloat(obj, v);
    }


    public static Method getMethod(Class<?> clazz, String name, Class<?>... params) throws NoSuchMethodException {
        if (clazz == null) throw new NoSuchMethodException("null class for method " + name);
        for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(name, params);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
            }
        }

        Method inherited = findInInterfaces(clazz, name, params);
        if (inherited != null) return inherited;

        Log.debug("Method not found: " + name + " in " + clazz.getName());
        throw new NoSuchMethodException(name);
    }

    private static Method findInInterfaces(Class<?> type, String name, Class<?>... params) {
        if (type == null) return null;
        for (Class<?> iface : type.getInterfaces()) {
            try {
                Method method = iface.getDeclaredMethod(name, params);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
            }
            Method nested = findInInterfaces(iface, name, params);
            if (nested != null) return nested;
        }
        return null;
    }

    public static Object invokeMethod(Method m, Object instance, Object... args) throws Exception {
        if (m == null) return null;
        m.setAccessible(true);
        return m.invoke(instance, args);
    }

    public static Object invokeStaticMethod(Method m, Object... args) throws Exception {
        if (m == null) return null;
        m.setAccessible(true);
        return m.invoke(null, args);
    }

    public static Object invokeStaticMethod(String className, String methodName, Class<?>[] paramTypes, Object... args)
            throws Exception {
        Method method = getClass(className).getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(null, args);
    }


    public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... paramTypes) throws NoSuchMethodException {
        Constructor<?> constructor = clazz.getDeclaredConstructor(paramTypes);
        constructor.setAccessible(true);
        return constructor;
    }

    public static Object invokeConstructor(Constructor<?> c, Object... args) throws Exception {
        return c.newInstance(args);
    }


    public static List<Class<?>> getAllClassesInPackage(String pkg) throws Exception {
        List<Class<?>> result = new ArrayList<>();
        for (String name : Resources.classesIn(pkg)) {
            result.add(Class.forName(name, true, getGameLoader()));
        }
        return result;
    }
}
