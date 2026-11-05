
package dev.nebula.client.injection.transformers;

import dev.nebula.client.injection.Targets;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
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

public final class NetHandlerPlayClientTransformer extends HookTransformer {

    private static final String METHOD = "handleEntityMovement";
    private static final String DESC =
            "(L" + Targets.S14_PACKET_ENTITY + ";)V";

    private static final String EVENT =
            "dev/nebula/client/event/impl/entity/EntityMovementEvent";

    private static final String EVENT_MANAGER =
            "dev/nebula/client/event/EventManager";

    private static final String WRAPPER_ENTITY =
            "dev/nebula/client/mappings/wrapper/entity/Entity";

    public NetHandlerPlayClientTransformer() {
        super(Targets.NET_HANDLER_PLAY_CLIENT);
    }

    @Override
    protected boolean patch(ClassNode node) {
        MethodNode method = find(node, METHOD, DESC);

        if (method == null) {
            Targets.reportMiss(
                    "NetHandlerPlayClientTransformer",
                    node,
                    METHOD,
                    DESC
            );
            return false;
        }

        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) insn;

                if (call.owner.equals(EVENT_MANAGER)
                        && call.name.equals("getInstance")
                        && call.desc.equals(
                                "()L" + EVENT_MANAGER + ";")) {
                    return false;
                }
            }
        }

        int entityVar = method.maxLocals;
        int wrapperVar = entityVar + 1;
        int eventVar = wrapperVar + 1;

        method.maxLocals += 3;

        LabelNode skip = new LabelNode();
        InsnList hook = new InsnList();

        hook.add(new VarInsnNode(ALOAD, 1));

        hook.add(new VarInsnNode(ALOAD, 0));
        hook.add(new FieldInsnNode(
                GETFIELD,
                Targets.NET_HANDLER_PLAY_CLIENT,
                "clientWorldController",
                "L" + Targets.WORLD_CLIENT + ";"
        ));

        hook.add(new MethodInsnNode(
                INVOKEVIRTUAL,
                Targets.S14_PACKET_ENTITY,
                "getEntity",
                "(L" + Targets.WORLD + ";)L" + Targets.ENTITY + ";",
                false
        ));

        hook.add(new VarInsnNode(ASTORE, entityVar));

        hook.add(new VarInsnNode(ALOAD, entityVar));
        hook.add(new JumpInsnNode(IFNULL, skip));

        hook.add(new TypeInsnNode(NEW, WRAPPER_ENTITY));
        hook.add(new InsnNode(DUP));
        hook.add(new VarInsnNode(ALOAD, entityVar));

        hook.add(new MethodInsnNode(
                INVOKESPECIAL,
                WRAPPER_ENTITY,
                "<init>",
                "(Ljava/lang/Object;)V",
                false
        ));

        hook.add(new VarInsnNode(ASTORE, wrapperVar));

        hook.add(new TypeInsnNode(NEW, EVENT));
        hook.add(new InsnNode(DUP));
        hook.add(new VarInsnNode(ALOAD, wrapperVar));

        hook.add(new MethodInsnNode(
                INVOKESPECIAL,
                EVENT,
                "<init>",
                "(L" + WRAPPER_ENTITY + ";)V",
                false
        ));

        hook.add(new VarInsnNode(ASTORE, eventVar));

        hook.add(new MethodInsnNode(
                INVOKESTATIC,
                EVENT_MANAGER,
                "getInstance",
                "()L" + EVENT_MANAGER + ";",
                false
        ));

        hook.add(new VarInsnNode(ALOAD, eventVar));

        hook.add(new MethodInsnNode(
                INVOKEVIRTUAL,
                EVENT_MANAGER,
                "call",
                "(Ldev/nebula/client/event/Event;)V",
                false
        ));

        hook.add(skip);

        AbstractInsnNode anchor = findThreadCheck(method);

        if (anchor != null) {
            method.instructions.insert(anchor, hook);
        } else {
            System.err.println(
                    "[NetHandlerPlayClientTransformer] "
                    + "checkThreadAndEnqueue não encontrado; "
                    + "hook inserido no início do método."
            );

            method.instructions.insertBefore(
                    method.instructions.getFirst(),
                    hook
            );
        }

        patchSpawnPlayer(node);

        return true;
    }

    private void patchSpawnPlayer(ClassNode node) {
        for (MethodNode m : node.methods) {
            if ((m.name.equals("handleSpawnPlayer") || m.name.equals("a"))
                    && m.desc != null && m.desc.contains("S0CPacketSpawnPlayer")) {
                LabelNode start = new LabelNode();
                LabelNode end = new LabelNode();
                LabelNode handler = new LabelNode();
                m.instructions.insert(start);
                m.instructions.add(end);
                m.instructions.add(handler);
                m.instructions.add(new InsnNode(POP));
                m.instructions.add(new InsnNode(RETURN));
                m.tryCatchBlocks.add(new org.objectweb.asm.tree.TryCatchBlockNode(start, end, handler, "java/lang/Throwable"));
                break;
            }
        }
    }

    private static AbstractInsnNode findThreadCheck(MethodNode method) {
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) insn;

                if (call.getOpcode() == INVOKESTATIC
                        && call.name.equals("checkThreadAndEnqueue")) {
                    return call;
                }
            }
        }

        return null;
    }
}
