package com.github.kb1000.optimizer.optimizers;

import java.util.ArrayList;
import java.util.ListIterator;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.github.kb1000.optimizer.Optimizer;

import static org.objectweb.asm.Opcodes.*;

public final class RemoveConstantPOPs implements Optimizer {
    public final ClassNode transform(final ClassNode classNode) {
        final ArrayList<AbstractInsnNode> remove = new ArrayList<>(10);
        for (final MethodNode methodNode: classNode.methods) {
            final InsnList instructionList = methodNode.instructions;
            final ListIterator<AbstractInsnNode> iterator = methodNode.instructions.iterator();
            while (iterator.hasNext()) {
                final AbstractInsnNode instruction = iterator.next();
                if (instruction.getOpcode() == POP && instruction.getPrevious() != null) {
                    switch (instruction.getPrevious().getOpcode()) {
                    // FIXME(kb1000): add remaining
                    case LDC:
                        final Object cst = ((LdcInsnNode) instruction.getPrevious()).cst;
                        if (cst instanceof Long || cst instanceof Double) { // let the verifier handle it...
                            break;
                        }
                    case ICONST_M1:
                    case ICONST_0:
                    case ICONST_1:
                    case FLOAD:
                    case ALOAD:
                    case ILOAD: // loads aren't actually constant, but have no side effects
                    case DUP: // DUPs aren't constant either, but have no side effects if instantly POPed
                        remove.add(instruction.getPrevious());
                        iterator.remove();
                        if (Optimizer.DEBUG) {
                            System.err.println("Removed a constant POP in " + classNode.name + "->" + methodNode.name);
                        }
                    }
                }
            }

            for (final AbstractInsnNode instruction: remove) {
                instructionList.remove(instruction);
            }
            remove.clear();
        }
        return classNode;
    }
}
