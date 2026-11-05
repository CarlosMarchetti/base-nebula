package dev.nebula.client.injection;

import dev.nebula.client.injection.transformers.HookTransformer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.nio.charset.StandardCharsets;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class HookManager {

    private static final String OWN_PACKAGE = "dev/nebula/client/";
    private static final Map<String, List<ClassFileTransformer>> BY_TARGET = new ConcurrentHashMap<>();

    private HookManager() {}

    public static void register(HookTransformer transformer) {
        BY_TARGET.computeIfAbsent(transformer.target(), k -> new CopyOnWriteArrayList<>()).add(transformer);
    }

    public static void commit() {
        nSetTargets(BY_TARGET.keySet().toArray(new String[0]));

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) cl = HookManager.class.getClassLoader();

        for (Map.Entry<String, List<ClassFileTransformer>> entry : BY_TARGET.entrySet()) {
            String target = entry.getKey();
            String label = describe(entry.getValue()) + " em " + target;
            Class<?> loaded;
            try {
                loaded = Class.forName(target.replace('/', '.'), false, cl);
            } catch (ClassNotFoundException e) {
                dev.nebula.client.bridge.NativeBridge.log("[HookManager] " + label + " registrado (hook on load)");
                continue;
            } catch (Throwable t) {
                dev.nebula.client.bridge.NativeBridge.log("[HookManager] " + label + " erro ao resolver classe: " + t);
                continue;
            }
            int err = nRetransform(loaded);
            if (err == 0) {
                dev.nebula.client.bridge.NativeBridge.log("[HookManager] " + label + " retransformation returned OK (see transformer result)");
            } else {
                dev.nebula.client.bridge.NativeBridge.log("[HookManager] " + label + " FALHOU: " + errorName(err) + " (" + err + ")");
            }
        }
    }

    public static byte[] dispatch(ClassLoader loader, String name, Class<?> redefined,
                                  ProtectionDomain domain, byte[] data) {
        List<ClassFileTransformer> chain = BY_TARGET.get(name);
        if (chain == null) {
            return null;
        }
        byte[] current = data;
        for (ClassFileTransformer transformer : chain) {
            try {
                byte[] out = transformer.transform(loader, name, redefined, domain, current);
                if (out != null && out.length > 0) {
                    current = out;
                }
            } catch (IllegalClassFormatException | RuntimeException | LinkageError e) {
                System.out.println("[HookManager] " + transformer.getClass().getSimpleName()
                        + " falhou em " + name + ": " + e);
                e.printStackTrace();
            }
        }
        return current == data ? null : current;
    }

    private static boolean alreadyHooked(byte[] data) {
        return new String(data, StandardCharsets.ISO_8859_1).contains(OWN_PACKAGE);
    }

    private static String describe(List<ClassFileTransformer> chain) {
        StringBuilder sb = new StringBuilder();
        for (ClassFileTransformer t : chain) {
            if (sb.length() > 0) sb.append('+');
            sb.append(t.getClass().getSimpleName());
        }
        return sb.toString();
    }

    private static String errorName(int code) {
        switch (code) {
            case 21: return "INVALID_CLASS";
            case 22: return "CLASS_NOT_PREPARED";
            case 60: return "INVALID_CLASS_FORMAT";
            case 61: return "CIRCULAR_CLASS_DEFINITION";
            case 62: return "FAILS_VERIFICATION";
            case 63: return "UNSUPPORTED_REDEFINITION_METHOD_ADDED";
            case 64: return "UNSUPPORTED_REDEFINITION_SCHEMA_CHANGED";
            case 66: return "UNSUPPORTED_REDEFINITION_HIERARCHY_CHANGED";
            case 67: return "UNSUPPORTED_REDEFINITION_METHOD_DELETED";
            case 68: return "UNSUPPORTED_VERSION";
            case 69: return "NAMES_DONT_MATCH";
            case 70: return "UNSUPPORTED_REDEFINITION_CLASS_MODIFIERS_CHANGED";
            case 71: return "UNSUPPORTED_REDEFINITION_METHOD_MODIFIERS_CHANGED";
            case 72: return "UNSUPPORTED_REDEFINITION_CLASS_ATTRIBUTE_CHANGED";
            case 79: return "UNMODIFIABLE_CLASS";
            case 99: return "MUST_POSSESS_CAPABILITY";
            default: return "UNKNOWN";
        }
    }

    private static native void nSetTargets(String[] internalNames);

    private static native int nRetransform(Class<?> target);
}
