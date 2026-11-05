package dev.nebula.client.injection.transformers;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.tree.*;

public final class MovementInputTransformer extends HookTransformer {
    public MovementInputTransformer() {
        super("net/minecraft/client/entity/EntityPlayerSP");
    }

    protected boolean patch(ClassNode node) {
        MethodNode m = find(node, "onLivingUpdate", "()V");
        if (m == null) m = find(node, "func_70636_d", "()V");
        if (m == null) return false;
        for (AbstractInsnNode i : m.instructions)
            if (i instanceof MethodInsnNode
                    && ((MethodInsnNode) i).name.equals("movementInputReady")) return false;
        for (AbstractInsnNode i : m.instructions.toArray())
            if (i instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) i;
                if ((call.name.equals("updatePlayerMoveState") || call.name.equals("func_78898_a"))
                        && call.desc.equals("()V")) {
                    InsnList hook = new InsnList();
                    hook.add(new VarInsnNode(ALOAD, 0));
                    hook.add(
                            new MethodInsnNode(
                                    INVOKESTATIC,
                                    "dev/nebula/client/features/FeatureHooks",
                                    "movementInputReady",
                                    "(Ljava/lang/Object;)V",
                                    false));
                    m.instructions.insert(i, hook);
                    return true;
                }
            }
        return false;
    }
}
