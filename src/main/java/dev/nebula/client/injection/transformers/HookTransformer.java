package dev.nebula.client.injection.transformers;

import dev.nebula.client.injection.SafeClassWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public abstract class HookTransformer implements ClassFileTransformer {

    private final String target;

    protected HookTransformer(String targetInternalName) {
        this.target = targetInternalName;
    }

    public final String target() {
        return target;
    }

    protected abstract boolean patch(ClassNode node);

    protected boolean recomputeFrames() {
        return true;
    }

    @Override
    public final byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                  ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        String name = className;
        if (name == null && classBeingRedefined != null) {
            name = classBeingRedefined.getName().replace('.', '/');
        }
        if (!target.equals(name)) return null;
        try {
            boolean frames = recomputeFrames();
            ClassNode node = new ClassNode(org.objectweb.asm.Opcodes.ASM9);
            new ClassReader(classfileBuffer).accept(node, frames ? ClassReader.SKIP_FRAMES : 0);
            if (!patch(node)) {
                dev.nebula.client.bridge.NativeBridge.log("[" + getClass().getSimpleName() + "] no bytecode change in " + target);
                return null;
            }

            ClassLoader cl = loader != null ? loader : HookTransformer.class.getClassLoader();
            ClassWriter writer = frames
                    ? new SafeClassWriter(ClassWriter.COMPUTE_FRAMES, cl)
                    : new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            byte[] result = writer.toByteArray();

            compareStructure(classfileBuffer, result);
            dev.nebula.client.bridge.NativeBridge.log("[" + getClass().getSimpleName() + "] bytecode gerado: " + result.length + " bytes");
            return result;
        } catch (Throwable t) {
            System.err.println("[" + getClass().getSimpleName() + "] FALHA TOTAL em " + target);
            dev.nebula.client.bridge.NativeBridge.log("[" + getClass().getSimpleName() + "] FAILED: " + t);
            t.printStackTrace();
            return null;
        }
    }

    private void compareStructure(byte[] before, byte[] after) {
        Map<String, String> a = structure(before);
        Map<String, String> b = structure(after);
        for (Map.Entry<String, String> e : a.entrySet()) {
            String other = b.get(e.getKey());
            if (!e.getValue().equals(other)) {
                System.err.println("[" + getClass().getSimpleName() + "] estrutura alterada em " + target
                        + " [" + e.getKey() + "]\n  antes:  " + e.getValue() + "\n  depois: " + other);
            }
        }
    }

    private static Map<String, String> structure(byte[] data) {
        ClassNode n = new ClassNode(org.objectweb.asm.Opcodes.ASM9);
        new ClassReader(data).accept(n, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        Map<String, String> out = new TreeMap<>();
        out.put("header", n.version + "|" + n.access + "|" + n.name + "|" + n.superName + "|" + n.interfaces);
        List<String> inner = new ArrayList<>();
        for (InnerClassNode ic : n.innerClasses) {
            inner.add(ic.name + "|" + ic.outerName + "|" + ic.innerName + "|" + ic.access);
        }
        out.put("innerClasses", inner.toString());
        out.put("enclosing", n.outerClass + "|" + n.outerMethod + "|" + n.outerMethodDesc);
        TreeSet<String> fields = new TreeSet<>();
        for (FieldNode f : n.fields) fields.add(f.access + "|" + f.name + "|" + f.desc);
        out.put("fields", fields.toString());
        TreeSet<String> methods = new TreeSet<>();
        for (MethodNode m : n.methods) methods.add(m.access + "|" + m.name + "|" + m.desc);
        out.put("methods", methods.toString());
        return out;
    }

    protected static MethodNode find(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) {
                return method;
            }
        }
        return null;
    }
}
