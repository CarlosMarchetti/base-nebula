package dev.nebula.client.injection.transformers;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.tree.*;

public final class InventorySyncTransformer extends HookTransformer {
    public InventorySyncTransformer() {
        super("net/minecraft/client/network/NetHandlerPlayClient");
    }

    protected boolean patch(ClassNode node) {
        String[] names = {
            "handleConfirmTransaction", "handleWindowItems", "handleSetSlot", "handleEntityVelocity"
        };
        String[] aliases = {"func_147239_a", "func_147241_a", "func_147266_a", "func_147244_a"};
        String[] packets = {
            "S32PacketConfirmTransaction",
            "S30PacketWindowItems",
            "S2FPacketSetSlot",
            "S12PacketEntityVelocity"
        };
        boolean changed = false;
        for (int i = 0; i < names.length; i++) {
            String desc = "(Lnet/minecraft/network/play/server/" + packets[i] + ";)V";
            MethodNode m = find(node, names[i], desc);
            if (m == null) m = find(node, aliases[i], desc);
            if (m == null) continue;
            boolean installed = false;
            for (AbstractInsnNode ins : m.instructions)
                if (ins instanceof MethodInsnNode
                        && ((MethodInsnNode) ins).name.equals("inventoryReady")) installed = true;
            if (installed) continue;
            for (AbstractInsnNode ins : m.instructions.toArray())
                if (ins.getOpcode() == RETURN) {
                    InsnList hook = new InsnList();
                    hook.add(new VarInsnNode(ALOAD, 1));
                    hook.add(new LdcInsnNode(i));
                    hook.add(
                            new MethodInsnNode(
                                    INVOKESTATIC,
                                    "dev/nebula/client/features/FeatureHooks",
                                    "inventoryReady",
                                    "(Ljava/lang/Object;I)V",
                                    false));
                    m.instructions.insertBefore(ins, hook);
                    changed = true;
                }
        }
        return changed;
    }
}
