package dev.nebula.client.modules.combat.target;

public enum TargetMode {
    LOW_ARMOR("Low Armor"),
    BREAK_ARMOR("Break Armor"),
    HEALTH("Health");

    private final String displayName;

    TargetMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TargetMode fromDisplayName(String name) {
        if (name == null) return LOW_ARMOR;
        for (TargetMode mode : values()) {
            if (mode.displayName.equalsIgnoreCase(name) || mode.name().equalsIgnoreCase(name)) {
                return mode;
            }
        }
        return LOW_ARMOR;
    }

    public static String[] getNames() {
        TargetMode[] values = values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].displayName;
        }
        return names;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
