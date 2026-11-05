package dev.nebula.client.utils.combat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ArmorUtils {

    private ArmorUtils() {}

    public static int getEquippedArmorMask(Object playerHandle) {
        if (playerHandle == null) return 0;
        int mask = 0;
        for (int slot = 0; slot < 4; slot++) {
            Object stack = getArmorInSlot(playerHandle, slot);
            if (stack != null) {
                mask |= (1 << slot);
            }
        }
        return mask;
    }

    public static int getEquippedPiecesCount(Object playerHandle) {
        return Integer.bitCount(getEquippedArmorMask(playerHandle));
    }

    public static float getTotalArmorDurability(Object playerHandle) {
        if (playerHandle == null) return Float.MAX_VALUE;
        float remainingDurability = 0.0f;
        for (int slot = 0; slot < 4; slot++) {
            Object stack = getArmorInSlot(playerHandle, slot);
            if (stack != null) {
                int max = getItemMaxDamage(stack);
                int dmg = getItemDamage(stack);
                if (max > 0) {
                    int safeDamage = Math.max(0, Math.min(max, dmg));
                    remainingDurability += (float) (max - safeDamage);
                }
            }
        }
        return remainingDurability;
    }

    public static float getBreakArmorScore(Object playerHandle) {
        float weakest = Float.MAX_VALUE;
        for (int slot = 0; slot < 4; slot++) {
            Object stack = getArmorInSlot(playerHandle, slot);
            int max = getItemMaxDamage(stack);
            if (stack != null && max > 0)
                weakest = Math.min(weakest, Math.max(0, max - getItemDamage(stack)));
        }
        return weakest;
    }

    public static Object getArmorInSlot(Object playerHandle, int slot) {
        if (playerHandle == null) return null;

        try {
            Method m = findMethod(playerHandle.getClass(), "getCurrentArmor", int.class);
            if (m != null) {
                return m.invoke(playerHandle, slot);
            }
        } catch (Throwable ignored) {
        }

        try {
            Field invField = findField(playerHandle.getClass(), "inventory");
            if (invField != null) {
                Object inv = invField.get(playerHandle);
                if (inv != null) {
                    Field armorInvField = findField(inv.getClass(), "armorInventory");
                    if (armorInvField != null) {
                        Object armorArray = armorInvField.get(inv);
                        if (armorArray instanceof Object[]) {
                            Object[] arr = (Object[]) armorArray;
                            if (slot >= 0 && slot < arr.length) {
                                return arr[slot];
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    public static int getItemMaxDamage(Object itemStack) {
        if (itemStack == null) return 0;
        try {
            Method m = findMethod(itemStack.getClass(), "getMaxDamage");
            if (m != null) {
                Object res = m.invoke(itemStack);
                if (res instanceof Number) return ((Number) res).intValue();
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    public static int getItemDamage(Object itemStack) {
        if (itemStack == null) return 0;
        try {
            Method m = findMethod(itemStack.getClass(), "getItemDamage");
            if (m != null) {
                Object res = m.invoke(itemStack);
                if (res instanceof Number) return ((Number) res).intValue();
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    public static int getHurtTime(Object entityHandle) {
        if (entityHandle == null) return 0;
        try {
            Field f = findField(entityHandle.getClass(), "hurtTime");
            if (f != null) return f.getInt(entityHandle);
        } catch (Throwable ignored) {
        }
        return 0;
    }


    private static final ClassValue<java.util.Map<String, java.util.Optional<Method>>> METHODS =
            new ClassValue<java.util.Map<String, java.util.Optional<Method>>>() {
                protected java.util.Map<String, java.util.Optional<Method>> computeValue(
                        Class<?> type) {
                    return new java.util.concurrent.ConcurrentHashMap<>();
                }
            };
    private static final ClassValue<java.util.Map<String, java.util.Optional<Field>>> FIELDS =
            new ClassValue<java.util.Map<String, java.util.Optional<Field>>>() {
                protected java.util.Map<String, java.util.Optional<Field>> computeValue(
                        Class<?> type) {
                    return new java.util.concurrent.ConcurrentHashMap<>();
                }
            };

    private static String alias(String name) {
        switch (name) {
            case "getCurrentArmor":
                return "func_82169_q";
            case "getMaxDamage":
                return "func_77958_k";
            case "getItemDamage":
                return "func_77960_j";
            case "inventory":
                return "field_71071_by";
            case "armorInventory":
                return "field_70460_b";
            case "hurtTime":
                return "field_70737_aN";
            default:
                return name;
        }
    }

    public static Method findMethod(Class<?> clazz, String name, Class<?>... params) {
        if (clazz == null) return null;
        String key = params.length == 0 ? name
                : name.equals("getCurrentArmor") && params.length == 1 && params[0] == int.class
                    ? "getCurrentArmor(int)" : name + java.util.Arrays.toString(params);
        java.util.Optional<Method> cached = METHODS.get(clazz).get(key);
        if (cached != null) return cached.orElse(null);
        Method result = null;
        for (Class<?> type = clazz; type != null && result == null; type = type.getSuperclass())
            for (String candidate : new String[] {name, alias(name)}) {
                try {
                    result = type.getDeclaredMethod(candidate, params);
                    result.setAccessible(true);
                    break;
                } catch (NoSuchMethodException ignored) {
                }
            }
        METHODS.get(clazz).put(key, java.util.Optional.ofNullable(result));
        return result;
    }

    public static Field findField(Class<?> clazz, String name) {
        if (clazz == null) return null;
        java.util.Optional<Field> cached = FIELDS.get(clazz).get(name);
        if (cached != null) return cached.orElse(null);
        Field result = null;
        for (Class<?> type = clazz; type != null && result == null; type = type.getSuperclass())
            for (String candidate : new String[] {name, alias(name)}) {
                try {
                    result = type.getDeclaredField(candidate);
                    result.setAccessible(true);
                    break;
                } catch (NoSuchFieldException ignored) {
                }
            }
        FIELDS.get(clazz).put(name, java.util.Optional.ofNullable(result));
        return result;
    }
}
