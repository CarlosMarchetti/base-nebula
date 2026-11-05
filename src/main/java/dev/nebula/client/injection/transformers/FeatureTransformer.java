package dev.nebula.client.injection.transformers;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.tree.*;

public final class FeatureTransformer extends HookTransformer {
    public enum Kind {
        WORLD,
        ENTITY,
        NAME,
        PLAYER_NAME,
        ATTACK
    }

    private final Kind kind;

    public FeatureTransformer(Kind kind) {
        super(target(kind));
        this.kind = kind;
    }

    private static String target(Kind kind) {
        switch (kind) {
            case WORLD:
                return "net/minecraft/client/renderer/RenderGlobal";
            case ENTITY:
                return "net/minecraft/client/renderer/entity/RenderManager";
            case NAME:
                return "net/minecraft/client/renderer/entity/RendererLivingEntity";
            case PLAYER_NAME:
                return "net/minecraft/client/renderer/entity/RenderPlayer";
            case ATTACK:
                return "net/minecraft/client/multiplayer/PlayerControllerMP";
            default:
                throw new AssertionError(kind);
        }
    }

    protected boolean patch(ClassNode node) {
        String descriptor, name, alias;
        switch (kind) {
            case WORLD:
                descriptor =
                        "(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V";
                name = "renderEntities";
                alias = "func_180446_a";
                break;
            case ENTITY:
                descriptor = "(Lnet/minecraft/entity/Entity;DDDFFZ)Z";
                name = "doRenderEntity";
                alias = "func_147939_a";
                break;
            case NAME:
                descriptor = "(Lnet/minecraft/entity/EntityLivingBase;)Z";
                name = "canRenderName";
                alias = "func_177070_b";
                break;
            case PLAYER_NAME:
                descriptor = "(Lnet/minecraft/client/entity/AbstractClientPlayer;)Z";
                name = "canRenderName";
                alias = "func_177070_b";
                break;
            case ATTACK:
                descriptor =
                        "(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/entity/Entity;)V";
                name = "attackEntity";
                alias = "func_78764_a";
                break;
            default:
                throw new AssertionError(kind);
        }
        MethodNode method = find(node, name, descriptor);
        if (method == null) method = find(node, alias, descriptor);
        if (method == null) {
            dev.nebula.client.injection.Targets.reportMiss(
                    "FeatureTransformer/" + kind, node, name, descriptor);
            return false;
        }
        for (AbstractInsnNode ins : method.instructions)
            if (ins instanceof MethodInsnNode
                    && ((MethodInsnNode) ins).owner.equals("dev/nebula/client/features/FeatureHooks"))
                return false;
        if (kind == Kind.WORLD) {
            for (AbstractInsnNode ins : method.instructions.toArray())
                if (ins.getOpcode() == RETURN) {
                    InsnList hook = new InsnList();
                    hook.add(new VarInsnNode(FLOAD, 3));
                    hook.add(
                            new MethodInsnNode(
                                    INVOKESTATIC,
                                    "dev/nebula/client/features/FeatureHooks",
                                    "worldRender",
                                    "(F)V",
                                    false));
                    method.instructions.insertBefore(ins, hook);
                }
        } else if (kind == Kind.ATTACK) {
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(ALOAD, 2));
            hook.add(
                    new MethodInsnNode(
                            INVOKESTATIC,
                            "dev/nebula/client/features/FeatureHooks",
                            "attacked",
                            "(Ljava/lang/Object;)V",
                            false));
            method.instructions.insert(hook);
        } else {
            InsnList hook = new InsnList();
            LabelNode vanilla = new LabelNode();
            hook.add(new VarInsnNode(ALOAD, 1));
            hook.add(
                    new MethodInsnNode(
                            INVOKESTATIC,
                            "dev/nebula/client/features/FeatureHooks",
                            kind == Kind.ENTITY ? "hide" : "hideName",
                            "(Ljava/lang/Object;)Z",
                            false));
            hook.add(new JumpInsnNode(IFEQ, vanilla));
            hook.add(new InsnNode(ICONST_0));
            hook.add(new InsnNode(IRETURN));
            hook.add(vanilla);
            method.instructions.insert(hook);
        }
        return true;
    }
}
