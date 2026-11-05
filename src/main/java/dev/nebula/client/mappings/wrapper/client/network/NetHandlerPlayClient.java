package dev.nebula.client.mappings.wrapper.client.network;

import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.mappings.wrapper.network.NetworkManager;
import dev.nebula.client.mappings.wrapper.network.Packet;
import dev.nebula.client.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class NetHandlerPlayClient {

    private final Object handle;

    public NetHandlerPlayClient(Object handle) {
        this.handle = handle;
    }

    public Object getHandle() {
        return handle;
    }

    public void sendPacket(Packet packet) {
        try {
            ReflectionUtils.invokeMethod(
                    (Method) Mappings.NET_HANDLER_ADD_TO_SEND_QUEUE,
                    handle,
                    packet.getHandle()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to send packet", e);
        }
    }

    public NetworkManager getNetworkManager() {
        try {
            Object raw = ReflectionUtils.invokeMethod((Method) Mappings.NET_HANDLER_GET_NETWORK_MANAGER, handle);
            return (raw != null) ? new NetworkManager(raw) : null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get NetworkManager", e);
        }
    }
}
