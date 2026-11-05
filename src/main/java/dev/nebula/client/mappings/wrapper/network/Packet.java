package dev.nebula.client.mappings.wrapper.network;

public class Packet {

    protected final Object handle;

    public Packet(Object handle) {
        this.handle = handle;
    }

    public Object getHandle() {
        return handle;
    }
}
