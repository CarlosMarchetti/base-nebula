package dev.nebula.client.security;

public final class VerificationState {

    private static volatile boolean verified = false;

    public static boolean isVerified() {
        return verified;
    }

    public static void setVerified(boolean value) {
        verified = value;
    }
}
