
package dev.nebula.client.injection;

import org.objectweb.asm.ClassWriter;

public final class SafeClassWriter extends ClassWriter {

    private final ClassLoader loader;

    public SafeClassWriter(int flags, ClassLoader loader) {
        super(flags);
        this.loader = loader;
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        try {
            Class<?> class1 = load(type1);
            Class<?> class2 = load(type2);

            if (class1.isAssignableFrom(class2)) {
                return type1;
            }

            if (class2.isAssignableFrom(class1)) {
                return type2;
            }

            if (class1.isInterface() || class2.isInterface()) {
                return "java/lang/Object";
            }

            do {
                class1 = class1.getSuperclass();

                if (class1 == null) {
                    return "java/lang/Object";
                }
            } while (!class1.isAssignableFrom(class2));

            return class1.getName().replace('.', '/');

        } catch (Throwable ignored) {
            return "java/lang/Object";
        }
    }

    private Class<?> load(String internalName)
            throws ClassNotFoundException {
        return Class.forName(
            internalName.replace('/', '.'),
            false,
            loader
        );
    }
}
