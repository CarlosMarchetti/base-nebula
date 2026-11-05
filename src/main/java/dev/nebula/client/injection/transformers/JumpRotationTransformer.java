package dev.nebula.client.injection.transformers;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

public final class JumpRotationTransformer extends HookTransformer {
    public JumpRotationTransformer() { super("net/minecraft/entity/EntityLivingBase"); }
    @Override protected boolean recomputeFrames() { return false; }
    @Override protected boolean patch(ClassNode node) {
        MethodNode method = find(node, "jump", "()V");
        if (method == null) return false;
        boolean changed = false;
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (!(insn instanceof FieldInsnNode) || insn.getOpcode() != GETFIELD) continue;
            FieldInsnNode field = (FieldInsnNode) insn;
            if (!field.name.equals("rotationYaw") || !field.desc.equals("F")) continue;
            InsnList yaw = new InsnList();
            yaw.add(new VarInsnNode(ALOAD, 0));
            yaw.add(new InsnNode(SWAP));
            yaw.add(new MethodInsnNode(INVOKESTATIC, "dev/nebula/client/utils/RotationUtils", "movementYaw", "(Ljava/lang/Object;F)F", false));
            method.instructions.insert(insn, yaw);
            changed = true;
        }
        return changed;
    }
}
