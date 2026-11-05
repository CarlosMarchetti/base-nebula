package dev.nebula.client.injection.transformers;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.tree.*;

public final class GuiCameraTransformer extends HookTransformer {
    public GuiCameraTransformer() {
        super("net/minecraft/entity/Entity");
    }

    protected boolean patch(ClassNode node) {
        MethodNode method = find(node, "setAngles", "(FF)V");
        if (method == null) method = find(node, "func_70082_c", "(FF)V");
        if (method == null) return false;
        for (AbstractInsnNode ins : method.instructions)
            if (ins instanceof MethodInsnNode && ((MethodInsnNode) ins).name.equals("blockCamera"))
                return false;
        InsnList hook = new InsnList();
        LabelNode vanilla = new LabelNode();
        hook.add(new VarInsnNode(ALOAD, 0));
        hook.add(
                new MethodInsnNode(
                        INVOKESTATIC,
                        "dev/nebula/client/features/FeatureHooks",
                        "blockCamera",
                        "(Ljava/lang/Object;)Z",
                        false));
        hook.add(new JumpInsnNode(IFEQ, vanilla));
        hook.add(new InsnNode(RETURN));
        hook.add(vanilla);
        method.instructions.insert(hook);
        return true;
    }
}
