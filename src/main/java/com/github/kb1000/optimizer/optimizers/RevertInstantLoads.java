package com.github.kb1000.optimizer.optimizers;

import java.util.ListIterator;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.github.kb1000.optimizer.Optimizer;

import static org.objectweb.asm.Opcodes.*;

// TODO: tag transformed instructions in TransformInstantLoads to avoid
// "reverting" instructions not transformed
public final class RevertInstantLoads implements Optimizer {
    public final ClassNode transform(final ClassNode classNode) {
        for (final MethodNode methodNode: classNode.methods) {
            final InsnList instructionList = methodNode.instructions;
            final ListIterator<AbstractInsnNode> iterator = methodNode.instructions.iterator();
            while (iterator.hasNext()) {
                final AbstractInsnNode instruction = iterator.next();
                if (instruction instanceof VarInsnNode && instruction.getPrevious() != null) {
                    final VarInsnNode store = (VarInsnNode) instruction;
                    if (!TransformInstantLoads.transformed.getOrDefault(store, false)) {
                        continue;
                    }
                    boolean removed = false;
                    switch (store.getPrevious().getOpcode()) {
                    case DUP:
                        switch (store.getOpcode()) {
                        case ASTORE:
                            instructionList.remove(instruction.getPrevious());
                            instructionList.insert(instruction, new VarInsnNode(ALOAD, store.var));
                            removed = true;
                            break;
                        case ISTORE:
                            instructionList.remove(instruction.getPrevious());
                            instructionList.insert(instruction, new VarInsnNode(ILOAD, store.var));
                            removed = true;
                            break;
                        default:
                            removed = false;
                        }
                        break;
                    default:
                        removed = false;
                    }
                    if (Optimizer.INSTANT_LOAD_DEBUG && removed) {
                        System.err.println("Transformed a combined store-load from an intermediate form to normal form in " + classNode.name + "->" + methodNode.name);
                    }
                }
            }
        }
        return classNode;
    }
}
