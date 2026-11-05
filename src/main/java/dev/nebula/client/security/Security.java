package dev.nebula.client.security;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class Security {

    private static String[] activeModules = {"killaura", "sprint", "clickgui"};

    public static Future<Boolean> verifyHWID() {
        setVerified(true);
        return CompletableFuture.completedFuture(true);
    }

    public static String[] getActiveModules() {
        return activeModules;
    }

    public static void disableMod(String reason) {
        setVerified(false);
        throw new RuntimeException("Mod disabled: " + reason);
    }

    public static boolean isVerified() {
        return VerificationState.isVerified();
    }

    public static void setVerified(boolean isVerified) {
        VerificationState.setVerified(isVerified);
    }
}
