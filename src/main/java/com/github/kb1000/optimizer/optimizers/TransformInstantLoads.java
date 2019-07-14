package com.github.kb1000.optimizer.optimizers;

import java.util.ListIterator;
import java.util.WeakHashMap;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.github.kb1000.optimizer.Optimizer;

import static org.objectweb.asm.Opcodes.*;

public final class TransformInstantLoads implements Optimizer {
    /* package-private */ static final WeakHashMap<VarInsnNode, Boolean> transformed = new WeakHashMap<>();

    public final ClassNode transform(final ClassNode classNode) {
        for (final MethodNode methodNode: classNode.methods) {
            final InsnList instructionList = methodNode.instructions;
            final ListIterator<AbstractInsnNode> iterator = methodNode.instructions.iterator();
            while (iterator.hasNext()) {
                final AbstractInsnNode instruction = iterator.next();
                if (instruction instanceof VarInsnNode && instruction.getNext() instanceof VarInsnNode) {
                    final VarInsnNode store = (VarInsnNode) instruction;
                    final VarInsnNode load = (VarInsnNode) (instruction.getNext());
                    if (store.var == load.var) {
                        if ((store.getOpcode() == ISTORE && load.getOpcode() == ILOAD)
                         || (store.getOpcode() == FSTORE && load.getOpcode() == FLOAD)) {
                            instructionList.insertBefore(store, new InsnNode(DUP));
                            iterator.next();
                            iterator.remove();
			    transformed.put(store, true);
                            if (Optimizer.INSTANT_LOAD_DEBUG) {
                                System.err.println("Transformed a combined store-load to an intermediate form in " + classNode.name + "->" + methodNode.name);
                            }
                        } else if ((store.getOpcode() == LSTORE && load.getOpcode() == LLOAD)
                                || (store.getOpcode() == DSTORE && load.getOpcode() == DLOAD)) {
                            instructionList.insertBefore(store, new InsnNode(DUP2));
                            iterator.next();
                            iterator.remove();
                            if (Optimizer.INSTANT_LOAD_DEBUG) {
                                System.err.println("Transformed a combined store-load to an intermediate form in " + classNode.name + "->" + methodNode.name);
                            }
                        }
                    }
                    
                }
            }
        }
        return classNode;
    }
}
