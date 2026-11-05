package dev.nebula.client.game;

import dev.nebula.client.injection.Resources;
import com.google.gson.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.objectweb.asm.Type;

public final class MappedAccess {
    public static final class Missing extends RuntimeException {
        public Missing(String message, Throwable cause) {
            super(message, cause);
        }

        public Missing(String message) {
            super(message);
        }
    }

    private final Map<String, JsonObject> classes = new HashMap<>();
    private final Map<String, Class<?>> types = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Field>> fields = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Method>> methods = new ConcurrentHashMap<>();

    public MappedAccess() {
        try (InputStream in = Resources.open("/mappings/mcp-1.8.9.json")) {
            if (in == null) throw new Missing("Canonical mapping profile missing");
            JsonObject profile =
                    new JsonParser()
                            .parse(new InputStreamReader(in, StandardCharsets.UTF_8))
                            .getAsJsonObject();
            for (JsonElement c : profile.getAsJsonArray("classes"))
                classes.put(c.getAsJsonObject().get("name").getAsString(), c.getAsJsonObject());
        } catch (IOException e) {
            throw new Missing("Cannot read mappings", e);
        }
    }

    public Class<?> type(String logical) {
        Class<?> cached = types.get(logical);
        if (cached != null) return cached;
        JsonObject entry = classes.get(logical);
        if (entry == null) throw new Missing("Unknown mapped class: " + logical);
        try {
            Class<?> value =
                    Class.forName(
                            entry.get("obfuscatedName").getAsString().replace('/', '.'),
                            false,
                            MappedAccess.class.getClassLoader());
            types.put(logical, value);
            return value;
        } catch (ClassNotFoundException e) {
            throw new Missing("Missing game class: " + logical, e);
        }
    }

    public boolean is(Object value, String logical) {
        return value != null && type(logical).isInstance(value);
    }

    private JsonObject definition(String owner, String group, String name) {
        JsonObject c = classes.get(owner);
        if (c != null && c.has(group))
            for (JsonElement e : c.getAsJsonArray(group)) {
                JsonObject d = e.getAsJsonObject();
                if (d.get("name").getAsString().equals(name)) return d;
            }
        for (Class<?> parent = type(owner).getSuperclass();
                parent != null;
                parent = parent.getSuperclass()) {
            String internal = parent.getName().replace('.', '/');
            for (JsonObject definition : classes.values()) {
                if (!definition.get("obfuscatedName").getAsString().equals(internal)
                        || !definition.has(group)) continue;
                for (JsonElement e : definition.getAsJsonArray(group)) {
                    JsonObject member = e.getAsJsonObject();
                    if (member.get("name").getAsString().equals(name)) return member;
                }
            }
        }
        throw new Missing("Missing mapping: " + owner + "." + name);
    }

    private List<String> names(JsonObject d) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        result.add(d.get("obfuscatedName").getAsString());
        result.add(d.get("name").getAsString());
        if (d.has("aliases"))
            for (JsonElement a : d.getAsJsonArray("aliases")) result.add(a.getAsString());
        return new ArrayList<>(result);
    }

    public Field field(String owner, String name) {
        Map<String, Field> named = fields.get(owner);
        Field cached = named == null ? null : named.get(name);
        if (cached != null) return cached;
        JsonObject d = definition(owner, "fields", name);
        for (Class<?> c = type(owner); c != null; c = c.getSuperclass())
            for (String candidate : names(d))
                try {
                    Field f = c.getDeclaredField(candidate);
                    if (!Type.getDescriptor(f.getType()).equals(d.get("signature").getAsString()))
                        continue;
                    f.setAccessible(true);
                    fields.computeIfAbsent(owner, key -> new ConcurrentHashMap<>()).put(name, f);
                    return f;
                } catch (NoSuchFieldException ignored) {
                }
        throw new Missing("Unavailable field: " + owner + "." + name);
    }

    public Method method(String owner, String name) {
        Map<String, Method> named = methods.get(owner);
        Method cached = named == null ? null : named.get(name);
        if (cached != null) return cached;
        JsonObject d = definition(owner, "methods", name);
        for (Class<?> c = type(owner); c != null; c = c.getSuperclass())
            for (Method m : c.getDeclaredMethods())
                if (names(d).contains(m.getName())
                        && Type.getMethodDescriptor(m).equals(d.get("signature").getAsString())) {
                    m.setAccessible(true);
                    methods.computeIfAbsent(owner, key -> new ConcurrentHashMap<>()).put(name, m);
                    return m;
                }
        throw new Missing("Unavailable method: " + owner + "." + name);
    }

    public Object get(Object instance, String owner, String name) {
        try {
            return field(owner, name).get(instance);
        } catch (IllegalAccessException e) {
            throw new Missing(owner + "." + name, e);
        }
    }

    public void set(Object instance, String owner, String name, Object value) {
        try {
            field(owner, name).set(instance, value);
        } catch (IllegalAccessException e) {
            throw new Missing(owner + "." + name, e);
        }
    }

    public Object call(Object instance, String owner, String name, Object... args) {
        try {
            return method(owner, name).invoke(instance, args);
        } catch (ReflectiveOperationException e) {
            throw new Missing(owner + "." + name, e);
        }
    }

    public Object construct(String owner, Object... args) {
        Class<?> c = type(owner);
        for (Constructor<?> ctor : c.getDeclaredConstructors())
            if (ctor.getParameterCount() == args.length) {
                Class<?>[] p = ctor.getParameterTypes();
                boolean match = true;
                for (int i = 0; i < p.length; i++)
                    if (args[i] == null ? p[i].isPrimitive() : !boxed(p[i]).isInstance(args[i]))
                        match = false;
                if (match)
                    try {
                        ctor.setAccessible(true);
                        return ctor.newInstance(args);
                    } catch (ReflectiveOperationException e) {
                        throw new Missing("Constructor " + owner, e);
                    }
            }
        throw new Missing("Unavailable constructor: " + owner);
    }

    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == int.class) return Integer.class;
        if (type == short.class) return Short.class;
        if (type == long.class) return Long.class;
        if (type == boolean.class) return Boolean.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == byte.class) return Byte.class;
        return Character.class;
    }

    public String methodName(String owner, String name) {
        return method(owner, name).getName();
    }

    public String internalName(String owner) {
        return classes.get(owner).get("obfuscatedName").getAsString();
    }
}
