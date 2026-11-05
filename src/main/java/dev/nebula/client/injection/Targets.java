package dev.nebula.client.injection;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public final class Targets {

    private Targets() {}

    public static final String MINECRAFT = "net/minecraft/client/Minecraft";
    public static final String ENTITY = "net/minecraft/entity/Entity";
    public static final String ENTITY_PLAYER_SP = "net/minecraft/client/entity/EntityPlayerSP";
    public static final String NET_HANDLER_PLAY_CLIENT = "net/minecraft/client/network/NetHandlerPlayClient";
    public static final String NETWORK_MANAGER = "net/minecraft/network/NetworkManager";
    public static final String S14_PACKET_ENTITY = "net/minecraft/network/play/server/S14PacketEntity";
    public static final String PACKET = "net/minecraft/network/Packet";
    public static final String WORLD = "net/minecraft/world/World";
    public static final String WORLD_CLIENT = "net/minecraft/client/multiplayer/WorldClient";

    public static final String PACKET_DESC = "L" + PACKET + ";";

    public static void reportMiss(String tag, ClassNode node, String name, String desc) {
        System.err.println("[" + tag + "] alvo nao encontrado: " + node.name + "." + name + desc);
        for (MethodNode m : node.methods) {
            if (m.desc.equals(desc)) {
                System.err.println("[" + tag + "]   candidato: " + m.name + m.desc);
            }
        }
    }
}
