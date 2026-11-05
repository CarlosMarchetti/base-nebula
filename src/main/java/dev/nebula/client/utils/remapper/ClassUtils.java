package dev.nebula.client.utils.remapper;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public final class ClassUtils {

    private static final String OBJECT = "java/lang/Object";

    private ClassUtils() {}

    public static ClassNode toClassNode(byte[] bytecode) {
        ClassNode node = new ClassNode(Opcodes.ASM9);
        new ClassReader(bytecode).accept(node, ClassReader.SKIP_FRAMES);
        return node;
    }

    public static byte[] toBytes(ClassNode node) {
        ClassWriter writer = new HierarchyClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
    
    private static final class HierarchyClassWriter extends ClassWriter {

    	HierarchyClassWriter(int flags) {
    	    super(flags);
    	}

        @Override
        protected String getCommonSuperClass(String a, String b) {
            if (a.equals(b)) {
                return a;
            }
            ClassInfo infoA = read(a);
            ClassInfo infoB = read(b);
            if (infoA == null || infoB == null || infoA.isInterface || infoB.isInterface) {
                return OBJECT;
            }

            Set<String> ancestors = new HashSet<>();
            for (String name = a; name != null; ) {
                ancestors.add(name);
                ClassInfo info = read(name);
                name = info == null ? null : info.superName;
            }
            for (String name = b; name != null; ) {
                if (ancestors.contains(name)) {
                    return name;
                }
                ClassInfo info = read(name);
                name = info == null ? null : info.superName;
            }
            return OBJECT;
        }
    }

    private static final class ClassInfo {
        final String superName;
        final boolean isInterface;

        ClassInfo(String superName, boolean isInterface) {
            this.superName = superName;
            this.isInterface = isInterface;
        }
    }

    private static ClassInfo read(String internalName) {
        try (InputStream in = open(internalName + ".class")) {
            if (in == null) {
                return null;
            }
            ClassReader reader = new ClassReader(in);
            return new ClassInfo(reader.getSuperName(), (reader.getAccess() & Opcodes.ACC_INTERFACE) != 0);
        } catch (IOException e) {
            return null;
        }
    }

    private static InputStream open(String path) {
        ClassLoader loader = ClassUtils.class.getClassLoader();
        InputStream in = loader == null ? null : loader.getResourceAsStream(path);
        return in != null ? in : ClassLoader.getSystemResourceAsStream(path);
    }
}
