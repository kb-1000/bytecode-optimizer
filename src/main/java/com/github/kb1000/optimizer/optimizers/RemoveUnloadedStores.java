package com.github.kb1000.optimizer.optimizers;

import java.util.ArrayList;

import com.github.kb1000.optimizer.Optimizer;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import static org.objectweb.asm.Opcodes.*;

public final class RemoveUnloadedStores implements Optimizer {
    @Override
    public ClassNode transform(final ClassNode classNode) {
        final ArrayList<VarInsnNode> remove = new ArrayList<>(10);
        for (final MethodNode methodNode: classNode.methods) {
            boolean[] loaded = new boolean[methodNode.maxLocals + 16]; // add a few more since maxLocals is from the class file and may not be up to date
            for (final AbstractInsnNode instruction: methodNode.instructions) {
                if (instruction instanceof VarInsnNode) {
                    final VarInsnNode varInstruction = (VarInsnNode) instruction;
                    switch (varInstruction.getOpcode()) {
                    case ILOAD:
                    case FLOAD:
                    case ALOAD:
                        loaded[varInstruction.var] = true;
                        break;
                    case LLOAD:
                    case DLOAD:
                        loaded[varInstruction.var] = loaded[varInstruction.var + 1] = true;
                        break;
                    }
                }
            }

            for (final AbstractInsnNode instruction: methodNode.instructions) {
                if (instruction instanceof VarInsnNode) {
                    final VarInsnNode varInstruction = (VarInsnNode) instruction;
                    switch (varInstruction.getOpcode()) {
                    case ASTORE:
                    case ISTORE:
                    case FSTORE:
                        if (!loaded[varInstruction.var]) {
                            remove.add(varInstruction);
                            methodNode.instructions.insert(varInstruction, new InsnNode(POP));
                            if (Optimizer.DEBUG) {
                                System.out.println("Removed a never accessed 4 byte store in " + classNode.name + "->" + methodNode.name);
                            }
                        }
                        break;
                    case LSTORE:
                    case DSTORE:
                        if ((!loaded[varInstruction.var]) && (!loaded[varInstruction.var + 1])) {
                            remove.add(varInstruction);
                            methodNode.instructions.insert(varInstruction, new InsnNode(POP2));
                            if (Optimizer.DEBUG) {
                                System.out.println("Removed a never accessed 8 byte store in " + classNode.name + "->" + methodNode.name);
                            }
                        }
                        break;
                    }
                }
            }

            for (final VarInsnNode varInstruction: remove) {
                methodNode.instructions.remove(varInstruction);
            }
            remove.clear();
        }

        return classNode;
    }
}
