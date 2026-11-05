package dev.nebula.client.event.impl.entity;

import dev.nebula.client.event.Event;
import dev.nebula.client.mappings.wrapper.entity.Entity;

public class EntityMovementEvent extends Event {

    private final Entity entity;

    public EntityMovementEvent(
            dev.nebula.client.mappings.wrapper.entity.Entity entity) {
        this.entity = entity;
    }

    public dev.nebula.client.mappings.wrapper.entity.Entity getEntity() {
        return entity;
    }
}
