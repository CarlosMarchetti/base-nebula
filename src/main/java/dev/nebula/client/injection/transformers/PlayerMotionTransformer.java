package dev.nebula.client.injection.transformers;
import dev.nebula.client.injection.Targets;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

public final class PlayerMotionTransformer extends HookTransformer {
    private static final String HOOKS = "dev/nebula/client/utils/MotionHooks";
    public PlayerMotionTransformer() { super(Targets.ENTITY_PLAYER_SP); }
    @Override protected boolean recomputeFrames() { return false; }
    @Override protected boolean patch(ClassNode node) {
        MethodNode method = find(node, "onUpdateWalkingPlayer", "()V");
        if (method == null) method = find(node, "func_175161_p", "()V");
        if (method == null) { Targets.reportMiss("PlayerMotionTransformer", node, "onUpdateWalkingPlayer", "()V"); return false; }
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn instanceof MethodInsnNode && ((MethodInsnNode) insn).owner.equals(HOOKS)) return false;
        }
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn instanceof FieldInsnNode && insn.getOpcode() == GETFIELD) {
                FieldInsnNode field = (FieldInsnNode) insn;
                boolean yaw = field.name.equals("rotationYaw") || field.name.equals("field_70177_z");
                boolean pitch = field.name.equals("rotationPitch") || field.name.equals("field_70125_A");
                if (field.desc.equals("F") && (yaw || pitch)) {
                    InsnList override = new InsnList();
                    override.add(new VarInsnNode(ALOAD, 0)); override.add(new InsnNode(SWAP));
                    override.add(new MethodInsnNode(INVOKESTATIC, HOOKS, yaw ? "yaw" : "pitch", "(Ljava/lang/Object;F)F", false));
                    method.instructions.insert(insn, override);
                }
            }
            if (insn.getOpcode() == RETURN) {
                InsnList end = new InsnList(); end.add(new VarInsnNode(ALOAD, 0));
                end.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "end", "(Ljava/lang/Object;)V", false));
                method.instructions.insertBefore(insn, end);
            }
        }
        InsnList start = new InsnList(); start.add(new VarInsnNode(ALOAD, 0));
        start.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "begin", "(Ljava/lang/Object;)V", false));
        method.instructions.insert(start);
        return true;
    }
}
