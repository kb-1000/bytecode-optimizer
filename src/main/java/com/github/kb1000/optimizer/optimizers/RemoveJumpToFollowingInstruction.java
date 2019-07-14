package com.github.kb1000.optimizer.optimizers;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.github.kb1000.optimizer.Optimizer;

public final class RemoveJumpToFollowingInstruction implements Optimizer {
    @Override
    public ClassNode transform(final ClassNode classNode) {
        for (final MethodNode methodNode: classNode.methods) {
            for (final AbstractInsnNode instruction: methodNode.instructions) {
                if (instruction.getOpcode() == Opcodes.GOTO && ((JumpInsnNode) instruction).label == instruction.getNext()) {
                    methodNode.instructions.set(instruction, new InsnNode(Opcodes.NOP));
                    if (Optimizer.DEBUG) {
                        System.err.println("Removed a jump to the following instruction in " + classNode.name + "->" + methodNode.name);
                    }
                }
            }
        }
        return classNode;
    }
}
