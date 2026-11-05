package dev.nebula.client.utils.remapper;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

public class NodeUtils {
	public static InsnList toNodes(AbstractInsnNode... nodes) {
        final InsnList insnList = new InsnList();
        for (final AbstractInsnNode node : nodes) {
            insnList.add(node);
        }
        return insnList;
    }
}
