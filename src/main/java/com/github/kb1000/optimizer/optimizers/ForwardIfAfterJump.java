package com.github.kb1000.optimizer.optimizers;

import org.objectweb.asm.tree.*;

import com.github.kb1000.optimizer.Optimizer;
import com.github.kb1000.optimizer.OptimizerUtils;

import static org.objectweb.asm.Opcodes.*;

public final class ForwardIfAfterJump implements Optimizer {
    @Override
    public ClassNode transform(final ClassNode classNode) {
        for (final MethodNode methodNode: classNode.methods) {
        outer:
            for (final AbstractInsnNode instruction: methodNode.instructions) {
                if (instruction instanceof JumpInsnNode && instruction.getPrevious() instanceof VarInsnNode) {
                    if (OptimizerUtils.isConstantPushingInstruction(instruction.getPrevious().getPrevious())) {
                        long resolved = OptimizerUtils.resolveConstantPushingInstruction(instruction.getPrevious().getPrevious());
                        final JumpInsnNode jump = (JumpInsnNode) instruction;
                        final LabelNode label = jump.label;
                        final AbstractInsnNode jumpTarget = label.getNext();
                        if (!(jumpTarget instanceof VarInsnNode)) {
                            continue;
                        }
                        if (!OptimizerUtils.isMatchingIntegerStore(instruction.getPrevious(), jumpTarget)) {
                            continue;
                        }
                        // Now we know the value in resolved is on the top of the stack when jumpTarget.next is executed
                        final AbstractInsnNode postLoadInstruction = jumpTarget.getNext();
                        // If any of the if branches get executed, we can replace the jump target label of instruction (or in case of the JumpInsnNode, any of the case-labels)
                        if (postLoadInstruction instanceof LookupSwitchInsnNode) {
                            final LookupSwitchInsnNode lookupSwitchJumpTarget = ((LookupSwitchInsnNode) postLoadInstruction);
                            final int count = Math.min(lookupSwitchJumpTarget.keys.size(), lookupSwitchJumpTarget.labels.size());
                            for (int i = 0; i < count; i++) {
                                if (resolved == lookupSwitchJumpTarget.keys.get(i)) {
                                    jump.label = lookupSwitchJumpTarget.labels.get(i);
                                    if (Optimizer.DEBUG) {
                                        System.err.println("Forwarded a jump to a lookupswitch in " + classNode.name + "->" + methodNode.name);
                                    }
                                    continue outer;
                                }
                            }
                            jump.label = lookupSwitchJumpTarget.dflt;
                            continue outer;
                        } else if (postLoadInstruction instanceof JumpInsnNode) {
                            switch (postLoadInstruction.getOpcode()) {
                            case IFEQ:
                                if (resolved == 0) {
                                    jump.label = ((JumpInsnNode) postLoadInstruction).label;
                                } else {
                                    jump.label = OptimizerUtils.getLabelAfterInstruction(postLoadInstruction, methodNode.instructions);
                                }
                                if (Optimizer.DEBUG) {
                                    System.err.println("Forwarded a jump to an ifeq in " + classNode.name + "->" + methodNode.name);
                                }
                                continue outer;
                            case IFNE:
                                if (resolved != 0) {
                                    jump.label = ((JumpInsnNode) postLoadInstruction).label;
                                } else {
                                    jump.label = OptimizerUtils.getLabelAfterInstruction(postLoadInstruction, methodNode.instructions);
                                }
                                if (Optimizer.DEBUG) {
                                    System.err.println("Forwarded a jump to an ifne in " + classNode.name + "->" + methodNode.name);
                                }
                                continue outer;
                            }
                        } else if (postLoadInstruction instanceof TableSwitchInsnNode) {
                            final TableSwitchInsnNode tableSwitchJumpTarget = ((TableSwitchInsnNode) postLoadInstruction);
                            final int count = Math.min(tableSwitchJumpTarget.labels.size(), tableSwitchJumpTarget.max - tableSwitchJumpTarget.max);
                            for (int i = 0; i < count; i++) {
                                if (resolved == tableSwitchJumpTarget.min + i) {
                                    jump.label = tableSwitchJumpTarget.labels.get(i);
                                    if (Optimizer.DEBUG) {
                                        System.err.println("Forwarded a jump to a tableswitch in " + classNode.name + "->" + methodNode.name);
                                    }
                                    continue outer;
                                }
                            }
                            jump.label = tableSwitchJumpTarget.dflt;
                            continue outer;
                        } else if (OptimizerUtils.isConstantPushingInstruction(postLoadInstruction) && postLoadInstruction.getNext() instanceof JumpInsnNode) {
                            final LabelNode label2 = ((JumpInsnNode) (postLoadInstruction).getNext()).label;
                            switch (postLoadInstruction.getNext().getOpcode()) {
                            }
                        }
                    }
                }
            }
        }
        return classNode;
    }
}
