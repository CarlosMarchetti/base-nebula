package dev.nebula.client.modules.combat;

public enum AttackMode {
    SINGLE("Single"),
    MULTI("Multi");

    private final String displayName;

    AttackMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AttackMode fromDisplayName(String name) {
        if ("Multi".equalsIgnoreCase(name)) return MULTI;
        return SINGLE;
    }

    public static String[] getNames() {
        return new String[]{"Single", "Multi"};
    }

    @Override
    public String toString() {
        return displayName;
    }
}

