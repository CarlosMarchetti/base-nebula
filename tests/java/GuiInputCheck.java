import static org.objectweb.asm.Opcodes.*;

import dev.nebula.client.injection.transformers.GuiCameraTransformer;
import dev.nebula.client.runtime.ClientContext;
import java.nio.file.*;
import org.objectweb.asm.*;

public class GuiInputCheck {
    static class Loader extends ClassLoader {
        Class<?> define(byte[] bytes) {
            return defineClass(null, bytes, 0, bytes.length);
        }
    }

    static void check(boolean b, String message) {
        if (!b) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        GameFeaturesCheck.FakeGame game = new GameFeaturesCheck.FakeGame();
        ClientContext context = GameFeaturesCheck.install(game, Paths.get(args[0]));
        ClassWriter w = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        String owner = "net/minecraft/entity/Entity";
        w.visit(V1_8, ACC_PUBLIC, owner, null, "java/lang/Object", null);
        w.visitField(ACC_PUBLIC, "yaw", "F", null, null).visitEnd();
        MethodVisitor ctor = w.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        ctor.visitCode();
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        ctor.visitInsn(RETURN);
        ctor.visitMaxs(0, 0);
        ctor.visitEnd();
        MethodVisitor m = w.visitMethod(ACC_PUBLIC, "setAngles", "(FF)V", null, null);
        m.visitCode();
        m.visitVarInsn(ALOAD, 0);
        m.visitVarInsn(FLOAD, 1);
        m.visitFieldInsn(PUTFIELD, owner, "yaw", "F");
        m.visitInsn(RETURN);
        m.visitMaxs(0, 0);
        m.visitEnd();
        w.visitEnd();
        GuiCameraTransformer transform = new GuiCameraTransformer();
        byte[] bytes =
                transform.transform(
                        GuiInputCheck.class.getClassLoader(), owner, null, null, w.toByteArray());
        check(bytes != null, "Camera hook missed");
        check(
                transform.transform(GuiInputCheck.class.getClassLoader(), owner, null, null, bytes)
                        == null,
                "Camera hook doubled");
        Class<?> type = new Loader().define(bytes);
        Object local = type.newInstance(), other = type.newInstance();
        game.player = local;
        java.lang.reflect.Method angles = type.getMethod("setAngles", float.class, float.class);
        context.guiInput().request(true);
        check(
                context.guiInput().blocksCamera(local),
                "Request did not immediately gate local camera");
        context.guiInput().apply();
        check(game.captured && context.guiInput().isCaptured(), "Vanilla menu not captured");
        angles.invoke(local, 15f, 0f);
        angles.invoke(other, 25f, 0f);
        check(
                type.getField("yaw").getFloat(local) == 0
                        && type.getField("yaw").getFloat(other) == 25,
                "Menu blocked wrong entity or leaked camera input");
        context.guiInput().request(false);
        context.guiInput().apply();
        angles.invoke(local, 30f, 0f);
        check(
                !game.captured && type.getField("yaw").getFloat(local) == 30,
                "Closing menu did not restore input");
        context.guiInput().request(true);
        context.guiInput().apply();
        game.captured = false;
        context.guiInput().apply();
        check(
                !context.guiInput().isCaptured(),
                "External screen replacement retained mouse ownership");
        System.out.println(
                "PASS immediate GUI gate, vanilla-screen ownership, local-only camera hook, close and external-screen cleanup");
    }
}
