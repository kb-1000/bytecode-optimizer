package com.github.kb1000.optimizer.optimizers;

import java.util.ListIterator;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import com.github.kb1000.optimizer.Optimizer;

import static org.objectweb.asm.Opcodes.*;

public final class RemoveInstructionsAfterReturn implements Optimizer {
    public final ClassNode transform(final ClassNode classNode) {
        for (final MethodNode method: classNode.methods) {
            final InsnList instructionList = method.instructions;
            final ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();
            while (iterator.hasNext()) {
                final AbstractInsnNode instruction = iterator.next();
                switch (instruction.getOpcode()) {
                case IRETURN:
                case ARETURN:
                case LRETURN:
                case RETURN:
                case FRETURN:
                case DRETURN:
                case ATHROW:
                    int i = 0;
                    while (iterator.hasNext() && !(iterator.next() instanceof LabelNode)) {
                        iterator.remove();
                        if (Optimizer.DEBUG) {
                            i++;
                        }
                    }
                    if (Optimizer.DEBUG && i != 0) {
                        System.err.println("Removed " + i + " instructions after return in " + classNode.name + "->" + method.name);
                    }
                }
            }
        }
        return classNode;
    }
}
