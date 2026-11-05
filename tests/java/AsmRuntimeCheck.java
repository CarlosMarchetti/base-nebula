import java.lang.reflect.*;
import java.util.List;
public final class AsmRuntimeCheck {
    public static void main(String[] args) throws Exception {
        String pkg = "dev.nebula.client.internal.asm9.";
        Class<?> writerClass = Class.forName(pkg + "ClassWriter");
        Class<?> readerClass = Class.forName(pkg + "ClassReader");
        Class<?> visitorClass = Class.forName(pkg + "ClassVisitor");
        Class<?> nodeClass = Class.forName(pkg + "tree.ClassNode");
        Object writer = writerClass.getConstructor(int.class).newInstance(0);
        writerClass.getMethod("visit",int.class,int.class,String.class,String.class,String.class,String[].class)
            .invoke(writer,55,1,"asmcheck/Outer",null,"java/lang/Object",null);
        writerClass.getMethod("visitNestMember",String.class).invoke(writer,"asmcheck/Outer$Inner");
        writerClass.getMethod("visitEnd").invoke(writer);
        byte[] bytes = (byte[])writerClass.getMethod("toByteArray").invoke(writer);
        Object reader = readerClass.getConstructor(byte[].class).newInstance((Object)bytes);
        Object node = nodeClass.getConstructor().newInstance();
        Field api = visitorClass.getDeclaredField("api");api.setAccessible(true);
        int asm9 = Class.forName(pkg+"Opcodes").getField("ASM9").getInt(null);
        if(api.getInt(node) != asm9) throw new IllegalStateException("Packaged ClassNode is not ASM9");
        readerClass.getMethod("accept",visitorClass,int.class).invoke(reader,node,0);
        List<?> members = (List<?>)nodeClass.getField("nestMembers").get(node);
        if(members==null || !members.contains("asmcheck/Outer$Inner")) throw new IllegalStateException("NestMember lost");
        Object resultWriter=writerClass.getConstructor(int.class).newInstance(0);
        nodeClass.getMethod("accept",visitorClass).invoke(node,resultWriter);
        System.out.println("PASS packaged ASM9: NestMember read and written successfully");
    }
}
