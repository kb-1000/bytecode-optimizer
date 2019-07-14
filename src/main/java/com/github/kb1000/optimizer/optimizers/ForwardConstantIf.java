package com.github.kb1000.optimizer.optimizers;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.github.kb1000.optimizer.Optimizer;
import com.github.kb1000.optimizer.OptimizerUtils;

import static org.objectweb.asm.Opcodes.*;

public final class ForwardConstantIf implements Optimizer {
    @Override
    public ClassNode transform(final ClassNode classNode) {
        for (final MethodNode methodNode: classNode.methods) {
            outer:
            for (final AbstractInsnNode instruction: methodNode.instructions) {
                if (instruction instanceof LineNumberNode || instruction instanceof LabelNode || instruction instanceof FrameNode) continue;
                final AbstractInsnNode store = OptimizerUtils.getNextUninterruptedInstruction(instruction);
                final AbstractInsnNode load = OptimizerUtils.getNextInstruction(store);
                if (OptimizerUtils.isConstantPushingInstruction(instruction) && OptimizerUtils.isMatchingIntegerStore(store, load)) {
                    final AbstractInsnNode postLoadInstruction = OptimizerUtils.getNextInstruction(load);
                    final long resolved = OptimizerUtils.resolveConstantPushingInstruction(instruction);
                    if (postLoadInstruction instanceof LookupSwitchInsnNode) {
                        final LookupSwitchInsnNode lookupSwitch = (LookupSwitchInsnNode) postLoadInstruction;
                        final int count = Math.min(lookupSwitch.keys.size(), lookupSwitch.labels.size());
                        for (int i = 0; i < count; i++) {
                            if (resolved == lookupSwitch.keys.get(i)) {
                                methodNode.instructions.insert(store, new JumpInsnNode(GOTO, lookupSwitch.labels.get(i)));
                                if (Optimizer.DEBUG) {
                                    System.err.println("Forwarded a constant lookupswitch to a jump in " + classNode.name + "->" + methodNode.name);
                                }
                                continue outer;
                            }
                        }
                        methodNode.instructions.insert(store, new JumpInsnNode(GOTO, lookupSwitch.dflt));
                        if (Optimizer.DEBUG) {
                            System.err.println("Forwarded a constant lookupswitch to a jump in " + classNode.name + "->" + methodNode.name);
                        }
                        continue outer;
                    }
                }
            }
        }
        return classNode;
    }
}
