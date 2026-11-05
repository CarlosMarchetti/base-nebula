package dev.nebula.client.injection.transformers;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.tree.*;

public final class ServerRotationTransformer extends HookTransformer {
    public ServerRotationTransformer() {
        super("net/minecraft/client/network/NetHandlerPlayClient");
    }

    protected boolean patch(ClassNode node) {
        String desc = "(Lnet/minecraft/network/play/server/S08PacketPlayerPosLook;)V";
        MethodNode method = find(node, "handlePlayerPosLook", desc);
        if (method == null) method = find(node, "func_147258_a", desc);
        if (method == null) return false;
        for (AbstractInsnNode ins : method.instructions)
            if (ins instanceof MethodInsnNode
                    && ((MethodInsnNode) ins).name.equals("serverRotationReady")) return false;
        for (AbstractInsnNode ins : method.instructions.toArray())
            if (ins.getOpcode() == RETURN)
                method.instructions.insertBefore(
                        ins,
                        new MethodInsnNode(
                                INVOKESTATIC,
                                "dev/nebula/client/features/FeatureHooks",
                                "serverRotationReady",
                                "()V",
                                false));
        return true;
    }
}
