package dev.nebula.client.mappings.wrapper.network;

import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.utils.ReflectionUtils;

import java.lang.reflect.Field;

public class NetworkManager {

    private final Object handle;

    public NetworkManager(Object handle) {
        this.handle = handle;
    }

    public Object getHandle() {
        return handle;
    }

    public Object getChannel() {
        try {
            return ReflectionUtils.getFieldValue((Field) Mappings.NETWORK_MANAGER_CHANNEL, handle);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get channel from NetworkManager", e);
        }
    }
}
