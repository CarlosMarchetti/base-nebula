package dev.nebula.client.event.impl;

import dev.nebula.client.event.Event;
import dev.nebula.client.mappings.wrapper.network.Packet;

public class PacketEvent extends Event {
    private final Packet packet;
    private final Type type;

    public PacketEvent(Packet packet, Type type) {
        this.packet = packet;
        this.type = type;
    }

    public Packet getPacket() {
        return packet;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        SEND, RECEIVE
    }
}
