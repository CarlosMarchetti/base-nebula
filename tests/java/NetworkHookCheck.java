import org.objectweb.asm.*;
import static org.objectweb.asm.Opcodes.*;
import dev.nebula.client.event.EventManager;
import dev.nebula.client.event.EventTarget;
import dev.nebula.client.event.impl.PacketEvent;
import dev.nebula.client.injection.transformers.NetworkTransformer;
import java.lang.reflect.*;
import java.util.*;

public final class NetworkHookCheck {
    public static final class Listener {
        boolean cancel;
        Object expected;
        final List<PacketEvent.Type> types=new ArrayList<>();
        @EventTarget public void packet(PacketEvent event) {
            if (event.getPacket().getHandle()!=expected) throw new AssertionError("Packet identity lost");
            types.add(event.getType()); event.setCancelled(cancel);
        }
    }
    static final class Loader extends ClassLoader {
        Class<?> define(byte[] bytes) { return defineClass(null,bytes,0,bytes.length); }
    }
    static void check(boolean success,String message) { if(!success)throw new AssertionError(message); }
    static void constructor(ClassWriter writer) {
        MethodVisitor m=writer.visitMethod(ACC_PUBLIC,"<init>","()V",null,null);
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESPECIAL,"java/lang/Object","<init>","()V",false);
        m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
    }
    static byte[] packet() {
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(V1_8,ACC_PUBLIC,"net/minecraft/network/Packet",null,"java/lang/Object",null);
        constructor(writer);writer.visitEnd();return writer.toByteArray();
    }
    static byte[] networkManager() {
        String owner="net/minecraft/network/NetworkManager";
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        writer.visit(V1_8,ACC_PUBLIC,owner,null,"java/lang/Object",null);constructor(writer);
        writer.visitField(ACC_PUBLIC,"sent","I",null,null).visitEnd();writer.visitField(ACC_PUBLIC,"received","I",null,null).visitEnd();
        String[] names={"sendPacket","channelRead0"}, counters={"sent","received"};
        String[] descriptors={"(Lnet/minecraft/network/Packet;)V","(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V"};
        for(int i=0;i<names.length;i++) {
            MethodVisitor m=writer.visitMethod(ACC_PUBLIC,names[i],descriptors[i],null,null);m.visitCode();
            m.visitVarInsn(ALOAD,0);m.visitInsn(DUP);m.visitFieldInsn(GETFIELD,owner,counters[i],"I");
            m.visitInsn(ICONST_1);m.visitInsn(IADD);m.visitFieldInsn(PUTFIELD,owner,counters[i],"I");
            m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        }
        writer.visitEnd();return writer.toByteArray();
    }
    public static void main(String[] args) throws Exception {
        Loader loader=new Loader();Class<?> packetClass=loader.define(packet());Object rawPacket=packetClass.newInstance();
        byte[] result=new NetworkTransformer().transform(loader,"net/minecraft/network/NetworkManager",null,null,networkManager());
        check(result!=null,"Network transformation failed");
        Class<?> network=loader.define(result);Object manager=network.newInstance();
        Method send=network.getMethod("sendPacket",packetClass);
        Method receive=network.getMethod("channelRead0",Class.forName("io.netty.channel.ChannelHandlerContext"),packetClass);
        Listener listener=new Listener();listener.expected=rawPacket;EventManager.getInstance().register(listener);
        try {
            send.invoke(manager,rawPacket);receive.invoke(manager,null,rawPacket);
            check(network.getField("sent").getInt(manager)==1,"Send did not reach original method");
            check(network.getField("received").getInt(manager)==1,"Receive did not reach original method");
            check(listener.types.equals(Arrays.asList(PacketEvent.Type.SEND,PacketEvent.Type.RECEIVE)),"Event type or wrapper incorrect");
            listener.cancel=true;send.invoke(manager,rawPacket);receive.invoke(manager,null,rawPacket);
            check(network.getField("sent").getInt(manager)==1,"Cancelled send reached original method");
            check(network.getField("received").getInt(manager)==1,"Cancelled receive reached original method");
            check(listener.types.size()==4,"Cancelled events not dispatched");
        } finally { EventManager.getInstance().unregister(listener); }
        System.out.println("PASS packaged network hooks: SEND/RECEIVE, raw packet identity, cancellation, no world/player required");
    }
}
