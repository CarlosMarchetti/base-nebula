package dev.nebula.client.utils;

public final class Log {

    public static boolean ENABLED = true;   
    public static boolean DEBUG = true;     

    private static final String PREFIX = "[Nebula] ";

    public static void info(String msg) {
        if (!ENABLED) return;
        System.out.println(PREFIX + msg);
    }

    public static void warn(String msg) {
        if (!ENABLED) return;
        System.out.println(PREFIX + "[WARN] " + msg);
    }

    public static void error(String msg) {
        if (!ENABLED) return;
        System.err.println(PREFIX + "[ERROR] " + msg);
    }

    public static void debug(String msg) {
        if (!DEBUG) return;
        System.out.println(PREFIX + "[DEBUG] " + msg);
    }
}
