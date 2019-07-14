package com.github.kb1000.optimizer.optimizers;

import java.util.ListIterator;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import com.github.kb1000.optimizer.Optimizer;

import static org.objectweb.asm.Opcodes.*;

public final class RemoveNOPs implements Optimizer {
    public final ClassNode transform(final ClassNode classNode) {
        for (final MethodNode method: classNode.methods) {
            final InsnList instructionList = method.instructions;
            final ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();
            while (iterator.hasNext()) {
                final AbstractInsnNode instruction = iterator.next();
                if (instruction.getOpcode() == NOP) {
                    iterator.remove();
                    if (Optimizer.DEBUG) {
                        System.err.println("Removed a NOP in " + classNode.name + "->" + method.name);
                    }
                }
            }
        }
        return classNode;
    }
}
