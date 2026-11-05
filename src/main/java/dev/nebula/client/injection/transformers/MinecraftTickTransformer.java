package dev.nebula.client.injection.transformers;

import dev.nebula.client.injection.Targets;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;

public final class MinecraftTickTransformer extends HookTransformer {

    private static final String METHOD = "runTick";
    private static final String DESC = "()V";

    public MinecraftTickTransformer() {
        super(Targets.MINECRAFT);
    }

    @Override
    protected boolean recomputeFrames() {
        return false;
    }

    @Override
    protected boolean patch(ClassNode node) {
        MethodNode method = find(node, METHOD, DESC);
        if (method == null) {
            Targets.reportMiss("MinecraftTickTransformer", node, METHOD, DESC);
            return false;
        }
        InsnList hook = new InsnList();
        hook.add(new MethodInsnNode(INVOKESTATIC, "dev/nebula/client/Main", "onClientTick", "()V", false));
        method.instructions.insert(hook);
        return true;
    }
}
