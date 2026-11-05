import static org.objectweb.asm.Opcodes.*;

import dev.nebula.client.injection.transformers.ServerRotationTransformer;
import dev.nebula.client.runtime.ClientContext;
import dev.nebula.client.utils.RotationUtils;
import java.nio.file.*;
import org.objectweb.asm.*;

public class ServerRotationCheck {
    static class Loader extends ClassLoader {
        Class<?> define(byte[] b) {
            return defineClass(null, b, 0, b.length);
        }
    }

    public static void main(String[] args) throws Exception {
        GameFeaturesCheck.FakeGame game = new GameFeaturesCheck.FakeGame();
        ClientContext context = GameFeaturesCheck.install(game, Paths.get(args[0]));
        String owner = "net/minecraft/client/network/NetHandlerPlayClient";
        ClassWriter w = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        w.visit(V1_8, ACC_PUBLIC, owner, null, "java/lang/Object", null);
        MethodVisitor ctor = w.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        ctor.visitCode();
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        ctor.visitInsn(RETURN);
        ctor.visitMaxs(0, 0);
        ctor.visitEnd();
        MethodVisitor method =
                w.visitMethod(
                        ACC_PUBLIC,
                        "handlePlayerPosLook",
                        "(Lnet/minecraft/network/play/server/S08PacketPlayerPosLook;)V",
                        null,
                        null);
        method.visitCode();
        method.visitInsn(RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
        w.visitEnd();
        ServerRotationTransformer transform = new ServerRotationTransformer();
        byte[] code =
                transform.transform(
                        ServerRotationCheck.class.getClassLoader(),
                        owner,
                        null,
                        null,
                        w.toByteArray());
        if (code == null
                || transform.transform(
                                ServerRotationCheck.class.getClassLoader(), owner, null, null, code)
                        != null)
            throw new AssertionError("Server correction hook missing/duplicated");
        Class<?> handler = new Loader().define(code);
        RotationUtils.setTargetRotation(100, 20);
        handler.getMethod(
                        "handlePlayerPosLook",
                        net.minecraft.network.play.server.S08PacketPlayerPosLook.class)
                .invoke(
                        handler.newInstance(),
                        new net.minecraft.network.play.server.S08PacketPlayerPosLook());
        if (RotationUtils.getTargetRotation() != null || RotationUtils.isReturning())
            throw new AssertionError("Server teleport retained stale client rotation");
        System.out.println(
                "PASS authoritative server correction, client-thread reset and idempotent post-vanilla hook");
    }
}
