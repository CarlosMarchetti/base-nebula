package dev.nebula.client.mappings.wrapper.item;

import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.utils.ReflectionUtils;

import java.lang.reflect.Method;

public class ItemStack {

    private final Object handle;

    public ItemStack(Object handle) {
        this.handle = handle;
    }

    public Object getHandle() {
        return handle;
    }

    public Object getItem() {
        try {
            return ReflectionUtils.invokeMethod(
                    (Method) Mappings.ITEM_STACK_GET_ITEM,
                    handle
            );
        } catch (Exception e) {
            return null;
        }
    }

    public int getItemId() {
        try {
            Object item = getItem();
            if (item == null) return -1;

            Object id = ReflectionUtils.invokeStaticMethod(
                    (Method) Mappings.ITEM_GET_ID_FROM_ITEM,
                    item
            );

            if (id instanceof Integer) {
                return (int) id;
            }

            return -1;
        } catch (Exception e) {
            return -1;
        }
    }

    public boolean isEmpty() {
        return handle == null || getItem() == null;
    }
}
