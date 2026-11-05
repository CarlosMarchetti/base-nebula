package dev.nebula.client.injection.transformers;

import dev.nebula.client.injection.Targets;
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

public final class NetworkTransformer extends HookTransformer {

    private static final String READ_METHOD = "channelRead0";
    private static final String READ_DESC = "(Lio/netty/channel/ChannelHandlerContext;" + Targets.PACKET_DESC + ")V";
    private static final String SEND_METHOD = "sendPacket";
    private static final String SEND_DESC = "(" + Targets.PACKET_DESC + ")V";

    private static final String EVENT = "dev/nebula/client/event/impl/PacketEvent";
    private static final String WRAPPER_PACKET = "dev/nebula/client/mappings/wrapper/network/Packet";
    private static final String EVENT_TYPE = "dev/nebula/client/event/impl/PacketEvent$Type";

    public NetworkTransformer() {
        super(Targets.NETWORK_MANAGER);
    }

    @Override
    protected boolean patch(ClassNode node) {
        boolean modified = false;

        MethodNode read = find(node, READ_METHOD, READ_DESC);
        if (read != null) {
            inject(read, 2, "RECEIVE");
            modified = true;
        } else {
            Targets.reportMiss("NetworkTransformer", node, READ_METHOD, READ_DESC);
        }

        MethodNode send = find(node, SEND_METHOD, SEND_DESC);
        if (send != null) {
            inject(send, 1, "SEND");
            modified = true;
        } else {
            Targets.reportMiss("NetworkTransformer", node, SEND_METHOD, SEND_DESC);
        }
        return modified;
    }

    private static void inject(MethodNode method, int packetSlot, String type) {
        int eventVar = method.maxLocals;
        method.maxLocals += 1;

        LabelNode proceed = new LabelNode();
        InsnList hook = new InsnList();

        hook.add(new TypeInsnNode(NEW, EVENT));
        hook.add(new InsnNode(DUP));
        hook.add(new TypeInsnNode(NEW, WRAPPER_PACKET));
        hook.add(new InsnNode(DUP));
        hook.add(new VarInsnNode(ALOAD, packetSlot));
        hook.add(new MethodInsnNode(INVOKESPECIAL, WRAPPER_PACKET, "<init>", "(Ljava/lang/Object;)V", false));
        hook.add(new FieldInsnNode(GETSTATIC, EVENT_TYPE, type, "L" + EVENT_TYPE + ";"));
        hook.add(new MethodInsnNode(INVOKESPECIAL, EVENT, "<init>",
                "(L" + WRAPPER_PACKET + ";L" + EVENT_TYPE + ";)V", false));
        hook.add(new VarInsnNode(ASTORE, eventVar));

        hook.add(new MethodInsnNode(INVOKESTATIC, "dev/nebula/client/event/EventManager", "getInstance",
                "()Ldev/nebula/client/event/EventManager;", false));
        hook.add(new VarInsnNode(ALOAD, eventVar));
        hook.add(new MethodInsnNode(INVOKEVIRTUAL, "dev/nebula/client/event/EventManager", "call",
                "(Ldev/nebula/client/event/Event;)V", false));

        hook.add(new VarInsnNode(ALOAD, eventVar));
        hook.add(new MethodInsnNode(INVOKEVIRTUAL, EVENT, "isCancelled", "()Z", false));
        hook.add(new JumpInsnNode(IFEQ, proceed));
        hook.add(new InsnNode(RETURN));
        hook.add(proceed);

        method.instructions.insertBefore(method.instructions.getFirst(), hook);
    }
}
