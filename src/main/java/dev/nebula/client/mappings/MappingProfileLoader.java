package dev.nebula.client.mappings;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import dev.nebula.client.injection.Resources;
import dev.nebula.client.utils.ReflectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MappingProfileLoader {

    private MappingProfileLoader() {}

    static void load(String resourcePath, Class<?> bindingsClass) throws Exception {
        InputStream stream = Resources.open(resourcePath);
        if (stream == null) {
            throw new IOException("Mapping profile not found: " + resourcePath);
        }

        try (InputStream input = stream;
             InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            Profile profile = new Gson().fromJson(reader, Profile.class);
            validateProfile(profile, resourcePath);
            bindProfile(profile, bindingsClass);
        }
    }

    private static void validateProfile(Profile profile, String resourcePath) {
        if (profile == null || profile.classes == null || profile.classes.isEmpty()) {
            throw new IllegalArgumentException("Mapping profile has no classes: " + resourcePath);
        }
        if (!"1.8.9".equals(profile.version)) {
            throw new IllegalArgumentException("Expected Minecraft 1.8.9 profile, got: " + profile.version);
        }
    }

    private static void bindProfile(Profile profile, Class<?> bindingsClass) throws Exception {
        Map<String, Class<?>> types = new LinkedHashMap<>();
        List<String> requiredFailures = new ArrayList<>();

        for (ClassEntry entry : profile.classes) {
            if (!hasBindings(entry)) {
                continue;
            }
            try {
                Class<?> type = resolveClass(entry);
                types.put(entry.name, type);
                if (entry.binding != null && !entry.binding.isEmpty()) {
                    bind(bindingsClass, entry.binding, type);
                }
            } catch (Exception e) {
                report(entry.required || hasRequiredBindings(entry), requiredFailures,
                        "class " + entry.name + " (" + runtimeClassName(entry) + ")", e);
            }
        }

        for (ClassEntry entry : profile.classes) {
            Class<?> owner = types.get(entry.name);
            if (owner == null) {
                continue;
            }
            bindMembers(bindingsClass, owner, entry.fields, MemberKind.FIELD, requiredFailures);
            bindMembers(bindingsClass, owner, entry.methods, MemberKind.METHOD, requiredFailures);
            bindMembers(bindingsClass, owner, entry.constructors, MemberKind.CONSTRUCTOR, requiredFailures);
        }

        if (!requiredFailures.isEmpty()) {
            throw new IllegalStateException("Required mappings missing:\n - " + String.join("\n - ", requiredFailures));
        }
    }

    private static void bindMembers(Class<?> bindingsClass, Class<?> owner, List<MemberEntry> entries,
                                    MemberKind kind, List<String> requiredFailures) throws Exception {
        if (entries == null) {
            return;
        }
        for (MemberEntry entry : entries) {
            if (entry.binding == null || entry.binding.isEmpty()) {
                continue;
            }
            try {
                Member member = resolveMember(owner, entry, kind);
                if (entry.binding != null && !entry.binding.isEmpty()) {
                    bind(bindingsClass, entry.binding, member);
                }
            } catch (Exception e) {
                report(entry.required, requiredFailures,
                        kind.name().toLowerCase() + " " + owner.getName() + "." + entry.name
                                + " " + entry.signature,
                        e);
            }
        }
    }

    private static Member resolveMember(Class<?> owner, MemberEntry entry, MemberKind kind) throws Exception {
        if (kind == MemberKind.METHOD && "<init>".equals(entry.name)) {
            return findConstructor(owner, entry);
        }
        List<String> names = candidateNames(entry);
        switch (kind) {
            case FIELD:
                return findField(owner, names, entry);
            case METHOD:
                return findMethod(owner, names, entry);
            case CONSTRUCTOR:
                return findConstructor(owner, entry);
            default:
                throw new IllegalArgumentException("Unsupported mapping kind: " + kind);
        }
    }

    private static Field findField(Class<?> owner, List<String> names, MemberEntry entry) throws Exception {
        for (Class<?> type = owner; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!names.contains(field.getName()) || !signatureMatches(entry.signature, descriptor(field.getType()))
                        || !staticMatches(entry.isStatic, Modifier.isStatic(field.getModifiers()))) {
                    continue;
                }
                field.setAccessible(true);
                return field;
            }
        }
        throw new NoSuchFieldException("No field matched names=" + names + ", descriptor=" + entry.signature);
    }

    private static Method findMethod(Class<?> owner, List<String> names, MemberEntry entry) throws Exception {
        for (Class<?> type = owner; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (!names.contains(method.getName()) || !signatureMatches(entry.signature, descriptor(method))
                        || !staticMatches(entry.isStatic, Modifier.isStatic(method.getModifiers()))) {
                    continue;
                }
                method.setAccessible(true);
                return method;
            }
        }
        for (Class<?> iface : owner.getInterfaces()) {
            Method method = findInterfaceMethod(iface, names, entry);
            if (method != null) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException("No method matched names=" + names + ", descriptor=" + entry.signature);
    }

    private static Method findInterfaceMethod(Class<?> iface, List<String> names, MemberEntry entry) {
        for (Method method : iface.getDeclaredMethods()) {
            if (names.contains(method.getName()) && signatureMatches(entry.signature, descriptor(method))
                    && staticMatches(entry.isStatic, Modifier.isStatic(method.getModifiers()))) {
                return method;
            }
        }
        for (Class<?> parent : iface.getInterfaces()) {
            Method method = findInterfaceMethod(parent, names, entry);
            if (method != null) {
                return method;
            }
        }
        return null;
    }

    private static Constructor<?> findConstructor(Class<?> owner, MemberEntry entry) throws Exception {
        for (Constructor<?> constructor : owner.getDeclaredConstructors()) {
            if (signatureMatches(entry.signature, descriptor(constructor))) {
                constructor.setAccessible(true);
                return constructor;
            }
        }
        throw new NoSuchMethodException("No constructor matched descriptor=" + entry.signature);
    }

    private static Class<?> resolveClass(ClassEntry entry) throws Exception {
        List<String> names = new ArrayList<>();
        addName(names, entry.obfuscatedName);
        addName(names, entry.runtimeName);
        addName(names, entry.name);
        Exception lastFailure = null;
        for (String name : names) {
            try {
                if (entry.noClassLoader) {
                    return Class.forName(name.replace('/', '.'), false, ClassLoader.getSystemClassLoader());
                }
                return ReflectionUtils.getClass(name.replace('/', '.'));
            } catch (Exception e) {
                lastFailure = e;
            }
        }
        throw lastFailure == null ? new ClassNotFoundException("No class names configured for " + entry.name) : lastFailure;
    }

    private static List<String> candidateNames(MemberEntry entry) {
        List<String> names = new ArrayList<>();
        addName(names, entry.obfuscatedName);
        addName(names, entry.runtimeName);
        addName(names, entry.name);
        if (entry.aliases != null) {
            for (String alias : entry.aliases) {
                addName(names, alias);
            }
        }
        return names;
    }

    private static void addName(List<String> names, String name) {
        if (name != null && !name.isEmpty() && !names.contains(name)) {
            names.add(name);
        }
    }

    private static boolean signatureMatches(String expected, String actual) {
        return expected == null || expected.isEmpty() || expected.equals(actual);
    }

    private static boolean staticMatches(Boolean expected, boolean actual) {
        return expected == null || expected.booleanValue() == actual;
    }

    private static String descriptor(Field field) {
        return descriptor(field.getType());
    }

    private static String descriptor(Method method) {
        StringBuilder result = new StringBuilder("(");
        for (Class<?> parameter : method.getParameterTypes()) {
            result.append(descriptor(parameter));
        }
        return result.append(')').append(descriptor(method.getReturnType())).toString();
    }

    private static String descriptor(Constructor<?> constructor) {
        StringBuilder result = new StringBuilder("(");
        for (Class<?> parameter : constructor.getParameterTypes()) {
            result.append(descriptor(parameter));
        }
        return result.append(")V").toString();
    }

    private static String descriptor(Class<?> type) {
        if (type.isArray()) {
            return type.getName().replace('.', '/');
        }
        if (!type.isPrimitive()) {
            return "L" + type.getName().replace('.', '/') + ";";
        }
        if (type == void.class) return "V";
        if (type == boolean.class) return "Z";
        if (type == byte.class) return "B";
        if (type == char.class) return "C";
        if (type == short.class) return "S";
        if (type == int.class) return "I";
        if (type == long.class) return "J";
        if (type == float.class) return "F";
        if (type == double.class) return "D";
        throw new IllegalArgumentException("Unsupported JVM type: " + type.getName());
    }

    private static String runtimeClassName(ClassEntry entry) {
        if (entry.obfuscatedName != null) return entry.obfuscatedName;
        return entry.runtimeName != null ? entry.runtimeName : entry.name;
    }

    private static boolean hasBindings(ClassEntry entry) {
        return (entry.binding != null && !entry.binding.isEmpty())
                || hasBindings(entry.fields)
                || hasBindings(entry.methods)
                || hasBindings(entry.constructors);
    }

    private static boolean hasRequiredBindings(ClassEntry entry) {
        return hasRequiredBindings(entry.fields)
                || hasRequiredBindings(entry.methods)
                || hasRequiredBindings(entry.constructors);
    }

    private static boolean hasRequiredBindings(List<MemberEntry> entries) {
        if (entries == null) return false;
        for (MemberEntry entry : entries) {
            if (entry.required && entry.binding != null && !entry.binding.isEmpty()) return true;
        }
        return false;
    }

    private static boolean hasBindings(List<MemberEntry> entries) {
        if (entries == null) return false;
        for (MemberEntry entry : entries) {
            if (entry.binding != null && !entry.binding.isEmpty()) return true;
        }
        return false;
    }

    private static void bind(Class<?> bindingsClass, String bindingName, Object value) throws Exception {
        Field target = bindingsClass.getField(bindingName);
        if (!Modifier.isStatic(target.getModifiers()) || !target.getType().isAssignableFrom(value.getClass())) {
            throw new IllegalArgumentException("Invalid mapping binding " + bindingName + " for " + value.getClass().getName());
        }
        target.set(null, value);
    }

    private static void report(boolean required, List<String> requiredFailures, String mapping, Exception cause) {
        String message = mapping + ": " + cause.getMessage();
        if (required) {
            requiredFailures.add(message);
        } else {
            System.err.println("[Mappings] optional mapping unavailable: " + message);
        }
    }

    private enum MemberKind { FIELD, METHOD, CONSTRUCTOR }

    private static final class Profile {
        String version;
        List<ClassEntry> classes;
        List<ClassEntry> optionalClasses;
    }

    private static final class ClassEntry {
        String name;
        String obfuscatedName;
        String runtimeName;
        String binding;
        boolean required;
        boolean noClassLoader;
        List<MemberEntry> fields;
        List<MemberEntry> methods;
        List<MemberEntry> constructors;
    }

    private static final class MemberEntry {
        String binding;
        String name;
        String obfuscatedName;
        String runtimeName;
        String signature;
        boolean required;
        @SerializedName("static") Boolean isStatic;
        List<String> aliases;
    }
}
