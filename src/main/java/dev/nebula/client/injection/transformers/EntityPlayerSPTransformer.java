package dev.nebula.client.injection.transformers;

import dev.nebula.client.injection.Targets;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import static org.objectweb.asm.Opcodes.*;

public final class EntityPlayerSPTransformer extends HookTransformer {

    private static final String METHOD = "onLivingUpdate";
    private static final String DESC = "()V";
    private static final String EVENT = "dev/nebula/client/event/impl/player/UpdateEvent";

    public EntityPlayerSPTransformer() {
        super(Targets.ENTITY_PLAYER_SP);
    }

    @Override
    protected boolean recomputeFrames() {
        return false;
    }

    @Override
    protected boolean patch(ClassNode node) {
        MethodNode method = find(node, METHOD, DESC);
        if (method == null) {
            Targets.reportMiss("EntityPlayerSPTransformer", node, METHOD, DESC);
            return false;
        }

        boolean inputHooked = false;
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (!(insn instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode)insn;
            if (call.desc.equals("()V") && (call.name.equals("updatePlayerMoveState") || call.name.equals("func_78898_a"))) {
                InsnList input = new InsnList(); input.add(new VarInsnNode(ALOAD,0));
                input.add(new MethodInsnNode(INVOKESTATIC,"dev/nebula/client/utils/MovementHooks","updateInput","(Ljava/lang/Object;)V",false));
                method.instructions.insert(insn,input); inputHooked=true;
            }
        }
        if (!inputHooked) dev.nebula.client.bridge.NativeBridge.log("EntityPlayerSPTransformer: movement-input update call not found");

        InsnList hook = new InsnList();
        hook.add(new TypeInsnNode(NEW, EVENT));
        hook.add(new InsnNode(DUP));
        hook.add(new MethodInsnNode(INVOKESPECIAL, EVENT, "<init>", "()V", false));
        hook.add(new MethodInsnNode(INVOKESTATIC, "dev/nebula/client/event/EventManager", "getInstance",
                "()Ldev/nebula/client/event/EventManager;", false));
        hook.add(new InsnNode(SWAP));
        hook.add(new MethodInsnNode(INVOKEVIRTUAL, "dev/nebula/client/event/EventManager", "call",
                "(Ldev/nebula/client/event/Event;)V", false));

        method.instructions.insertBefore(method.instructions.getFirst(), hook);
        return true;
    }
}
