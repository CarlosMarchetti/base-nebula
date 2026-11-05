package dev.nebula.client.injection;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Resources {

    private static final Map<String, byte[]> DATA = new ConcurrentHashMap<>();
    private static final Set<String> CLASSES = ConcurrentHashMap.newKeySet();

    private Resources() {}

    public static void put(String name, byte[] data) {
        DATA.put(name, data);
    }

    public static void registerClass(String internalName) {
        CLASSES.add(internalName);
    }

    public static InputStream open(String path) {
        String key = path.startsWith("/") ? path.substring(1) : path;
        byte[] data = DATA.get(key);
        return data == null ? null : new ByteArrayInputStream(data);
    }

    public static List<String> classesIn(String packageName) {
        String prefix = packageName.replace('.', '/') + "/";
        List<String> result = new ArrayList<>();
        for (String internal : CLASSES) {
            if (internal.startsWith(prefix)) {
                result.add(internal.replace('/', '.'));
            }
        }
        Collections.sort(result);
        return result;
    }
}
