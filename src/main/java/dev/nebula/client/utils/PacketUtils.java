package dev.nebula.client.utils;

import dev.nebula.client.mappings.wrapper.client.Minecraft;
import dev.nebula.client.mappings.wrapper.network.NetworkManager;
import dev.nebula.client.mappings.wrapper.network.Packet;
import io.netty.channel.Channel;

public final class PacketUtils {

    private PacketUtils() {}

    public static void sendPacket(Packet packet) {
        trySendPacket(packet);
    }

    public static boolean trySendPacket(Packet packet) {
        if (packet == null || packet.getHandle() == null) return false;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            dev.nebula.client.mappings.wrapper.client.network.NetHandlerPlayClient handler = mc.getNetHandler();
            if (handler == null || handler.getHandle() == null) {
                Log.warn("Packet not sent: NetHandler unavailable (" + packet.getClass().getSimpleName() + ")");
                return false;
            }
            if (dev.nebula.client.mappings.Mappings.NET_HANDLER_ADD_TO_SEND_QUEUE == null) {
                throw new IllegalStateException("NET_HANDLER_ADD_TO_SEND_QUEUE mapping unavailable");
            }
            handler.sendPacket(packet);
            return true;
        } catch (Throwable e) {
            Log.error("Erro ao enviar pacote " + packet.getClass().getSimpleName() + ": " + e);
            return false;
        }
    }

    public static void sendPacketNoEvent(Packet packet) {
        if (packet == null || packet.getHandle() == null) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null) return;

            NetworkManager networkManager = mc.getNetworkManager();
            if (networkManager != null) {
                Channel channel = (Channel) networkManager.getChannel();
                if (channel != null && channel.isOpen()) {
                    channel.writeAndFlush(packet.getHandle());
                    return;
                }
            }

            if (mc.getNetHandler() != null) {
                mc.getNetHandler().sendPacket(packet);
            }
        } catch (Throwable e) {
            Log.error("Erro no sendPacketNoEvent: " + e.getMessage());
        }
    }

    public static void sendPacketBypass(Packet packet) {
        sendPacketNoEvent(packet);
    }

    public static void flushQueue() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.getNetworkManager() != null) {
                Channel channel = (Channel) mc.getNetworkManager().getChannel();
                if (channel != null && channel.isOpen()) {
                    channel.flush();
                }
            }
        } catch (Throwable ignored) {}
    }
}
