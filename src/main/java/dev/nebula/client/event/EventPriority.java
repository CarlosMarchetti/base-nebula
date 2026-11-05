package dev.nebula.client.event;

public enum EventPriority {
    LOWEST(0),
    LOW(1),
    NORMAL(2),
    HIGH(3),
    HIGHEST(4);

    public final int value;

    EventPriority(int value) {
        this.value = value;
    }
}
