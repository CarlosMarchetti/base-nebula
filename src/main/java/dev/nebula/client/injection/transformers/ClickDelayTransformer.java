package dev.nebula.client.injection.transformers;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.tree.*;

public final class ClickDelayTransformer extends HookTransformer {
    public ClickDelayTransformer() {
        super("net/minecraft/client/Minecraft");
    }

    protected boolean patch(ClassNode node) {
        MethodNode m = find(node, "clickMouse", "()V");
        if (m == null) m = find(node, "func_147116_af", "()V");
        if (m == null) return false;
        for (AbstractInsnNode i : m.instructions)
            if (i instanceof MethodInsnNode
                    && ((MethodInsnNode) i).name.equals("beforeAttackClick")) return false;
        m.instructions.insert(
                new MethodInsnNode(
                        INVOKESTATIC,
                        "dev/nebula/client/features/FeatureHooks",
                        "beforeAttackClick",
                        "()V",
                        false));
        return true;
    }
}
