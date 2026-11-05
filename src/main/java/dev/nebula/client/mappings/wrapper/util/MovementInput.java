package dev.nebula.client.mappings.wrapper.util;

import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.utils.ReflectionUtils;

public class MovementInput {
    private final Object handle;
    
    public MovementInput(Object handle) {
        this.handle = handle;
    }
    
    public float getMoveForward() {
        try {
            return (float) ReflectionUtils.getFieldValue(Mappings.MOVE_FORWARD_FIELD, handle);
        } catch (Exception e) {
            return 0;
        }
    }
    
    public float getMoveStrafe() {
        try {
            return (float) ReflectionUtils.getFieldValue(Mappings.MOVE_STRAFING_FIELD, handle);
        } catch (Exception e) {
            return 0;
        }
    }
}
