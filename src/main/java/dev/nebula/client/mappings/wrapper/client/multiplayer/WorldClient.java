package dev.nebula.client.mappings.wrapper.client.multiplayer;

import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.mappings.wrapper.entity.Entity;
import dev.nebula.client.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class WorldClient {

    private final Object handle;

    public WorldClient(Object handle) {
        this.handle = handle;
    }

    public Object getHandle() {
        return handle;
    }

    public List<Entity> getLoadedEntityList() {
        try {
            Object rawList = ReflectionUtils.getFieldValue((Field) Mappings.WORLD_LOADED_ENTITY_LIST, handle);
            if (!(rawList instanceof List)) {
                return new ArrayList<>();
            }
            List<?> list = (List<?>) rawList;
            List<Entity> wrapped = new ArrayList<>(list.size());
            for (Object obj : list) {
                if (obj != null) {
                    wrapped.add(new Entity(obj));
                }
            }
            return wrapped;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get loaded entity list", e);
        }
    }

    public List<Entity> getPlayerEntities() {
        try {
            Object rawList = ReflectionUtils.getFieldValue((Field) Mappings.ENTITY_LIST_FIELD, handle);
            if (!(rawList instanceof List)) {
                return new ArrayList<>();
            }
            List<?> list = (List<?>) rawList;
            List<Entity> wrapped = new ArrayList<>(list.size());
            for (Object obj : list) {
                if (obj != null) {
                    wrapped.add(new Entity(obj));
                }
            }
            return wrapped;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get player entity list", e);
        }
    }
}
