package dev.nebula.client.injection.transformers;

import dev.nebula.client.injection.Targets;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import static org.objectweb.asm.Opcodes.*;

public final class MotionTransformer extends HookTransformer {

    private static final String METHOD = "moveFlying";
    private static final String DESC = "(FFF)V";
    private static final String EVENT = "dev/nebula/client/event/impl/player/StrafeEvent";

    public MotionTransformer() {
        super(Targets.ENTITY);
    }

    @Override
    protected boolean patch(ClassNode node) {
        MethodNode method = find(node, METHOD, DESC);
        if (method == null) {
            Targets.reportMiss("MotionTransformer", node, METHOD, DESC);
            return false;
        }

        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn instanceof FieldInsnNode && insn.getOpcode() == GETFIELD
                    && ((FieldInsnNode) insn).name.equals("rotationYaw") && ((FieldInsnNode) insn).desc.equals("F")) {
                InsnList yaw = new InsnList();
                yaw.add(new VarInsnNode(ALOAD, 0));
                yaw.add(new InsnNode(SWAP));
                yaw.add(new MethodInsnNode(INVOKESTATIC, "dev/nebula/client/utils/RotationUtils", "movementYaw", "(Ljava/lang/Object;F)F", false));
                method.instructions.insert(insn, yaw);
            }
        }

        int eventVar = method.maxLocals;
        method.maxLocals += 1;

        LabelNode skip = new LabelNode();
        InsnList hook = new InsnList();

        hook.add(new VarInsnNode(ALOAD, 0));
        hook.add(new TypeInsnNode(INSTANCEOF, Targets.ENTITY_PLAYER_SP));
        hook.add(new JumpInsnNode(IFEQ, skip));

        hook.add(new TypeInsnNode(NEW, EVENT));
        hook.add(new InsnNode(DUP));
        hook.add(new VarInsnNode(FLOAD, 1));
        hook.add(new VarInsnNode(FLOAD, 2));
        hook.add(new VarInsnNode(FLOAD, 3));
        hook.add(new MethodInsnNode(INVOKESPECIAL, EVENT, "<init>", "(FFF)V", false));
        hook.add(new VarInsnNode(ASTORE, eventVar));

        hook.add(new MethodInsnNode(INVOKESTATIC, "dev/nebula/client/event/EventManager", "getInstance",
                "()Ldev/nebula/client/event/EventManager;", false));
        hook.add(new VarInsnNode(ALOAD, eventVar));
        hook.add(new MethodInsnNode(INVOKEVIRTUAL, "dev/nebula/client/event/EventManager", "call",
                "(Ldev/nebula/client/event/Event;)V", false));

        hook.add(new VarInsnNode(ALOAD, eventVar));
        hook.add(new MethodInsnNode(INVOKEVIRTUAL, EVENT, "getStrafe", "()F", false));
        hook.add(new VarInsnNode(FSTORE, 1));
        hook.add(new VarInsnNode(ALOAD, eventVar));
        hook.add(new MethodInsnNode(INVOKEVIRTUAL, EVENT, "getForward", "()F", false));
        hook.add(new VarInsnNode(FSTORE, 2));
        hook.add(new VarInsnNode(ALOAD, eventVar));
        hook.add(new MethodInsnNode(INVOKEVIRTUAL, EVENT, "getFriction", "()F", false));
        hook.add(new VarInsnNode(FSTORE, 3));

        hook.add(skip);

        method.instructions.insertBefore(method.instructions.getFirst(), hook);
        return true;
    }
}
