package com.github.kb1000.optimizer.optimizers;

import static org.objectweb.asm.Opcodes.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.github.kb1000.optimizer.Optimizer;
import com.github.kb1000.optimizer.OptimizerUtils;

public final class ForwardDoubleJumps implements Optimizer {
    @Override
    public final ClassNode transform(final ClassNode classNode) {
        for (final MethodNode methodNode: classNode.methods) {
            final InsnList instructions = methodNode.instructions;
            for (final AbstractInsnNode instruction: instructions) {
                if (!(instruction instanceof JumpInsnNode))
                    continue;
                final JumpInsnNode jumpInstruction = (JumpInsnNode) instruction;
                while (true) {
                    final AbstractInsnNode jumpTarget = OptimizerUtils.getNextInstruction(jumpInstruction.label);
                    if (jumpTarget.getOpcode() != GOTO) {
                        break;
                    }

                    jumpInstruction.label = ((JumpInsnNode) jumpTarget).label;
                    if (Optimizer.DEBUG) {
                        System.err.println("Forwarded a double jump in " + classNode.name + "->" + methodNode.name);
                    }
                }
            }
        }

        // We modified it in-place
        return classNode;
    }
}
