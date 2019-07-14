package com.github.kb1000.optimizer;

import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

public final class OptimizerUtils {
    private static final boolean[] isBasicConstInstruction = new boolean[255];
    private static final long[] basicConstInstructionResult = new long[255];
    static {
        isBasicConstInstruction[ICONST_M1] = true;
        basicConstInstructionResult[ICONST_M1] = -1;
        isBasicConstInstruction[ICONST_0] = true;
        basicConstInstructionResult[ICONST_0] = 0;
        isBasicConstInstruction[ICONST_1] = true;
        basicConstInstructionResult[ICONST_1] = 1;
        isBasicConstInstruction[ICONST_2] = true;
        basicConstInstructionResult[ICONST_2] = 2;
        isBasicConstInstruction[ICONST_3] = true;
        basicConstInstructionResult[ICONST_3] = 3;
        isBasicConstInstruction[ICONST_4] = true;
        basicConstInstructionResult[ICONST_4] = 4;
        isBasicConstInstruction[ICONST_5] = true;
        basicConstInstructionResult[ICONST_5] = 5;
    }

    public static boolean isConstantPushingInstruction(final AbstractInsnNode instruction) {
        if (instruction instanceof InsnNode) {
            return isBasicConstInstruction[instruction.getOpcode()];
        } else if (instruction instanceof IntInsnNode) {
            return true;
        } else if (instruction instanceof LdcInsnNode) {
            final LdcInsnNode ldcInstruction = (LdcInsnNode) instruction;
            final Object param = ldcInstruction.cst;
            return (param instanceof Integer || param instanceof Long);
        }
        return false;
    }

    public static long resolveConstantPushingInstruction(final AbstractInsnNode instruction) {
        if (instruction instanceof InsnNode) {
            // It is undefined behavior to pass instructions to this that don't pass the check in the above method, so we can just return 0 here
            return basicConstInstructionResult[instruction.getOpcode()];
        } else if (instruction instanceof IntInsnNode) {
            return ((IntInsnNode) instruction).operand;
        } else if (instruction instanceof LdcInsnNode) {
            final Object constant = ((LdcInsnNode) instruction).cst;
            if (constant instanceof Integer) {
                return (int) constant;
            } else if (constant instanceof Long) {
                return (long) constant;
            }
        }
        throw new IllegalArgumentException("" + instruction);
    }

    public static boolean isMatchingIntegerStore(final AbstractInsnNode store, final AbstractInsnNode load) {
        if (!(store instanceof VarInsnNode && load instanceof VarInsnNode)) {
            return false;
        }
        return ((store.getOpcode() == ISTORE && load.getOpcode() == ILOAD) || (store.getOpcode() == LSTORE && load.getOpcode() == LLOAD)) && (((VarInsnNode) store).var == ((VarInsnNode) load).var);
    }

    public static LabelNode getLabelAfterInstruction(AbstractInsnNode instruction, InsnList list) {
        if (instruction instanceof LabelNode) return (LabelNode) instruction;
        if (instruction.getNext() instanceof LabelNode) return (LabelNode) (instruction.getNext());
        else {
            final LabelNode labelNode = new LabelNode();
            list.insert(instruction, labelNode);
            return labelNode;
        }
    }

    public static AbstractInsnNode getNextUninterruptedInstruction(AbstractInsnNode instruction) {
        if (instruction == null) return null;
        instruction = instruction.getNext();
        while (instruction instanceof LineNumberNode || instruction instanceof FrameNode) {
            instruction = instruction.getNext();
        }
        return instruction;
    }

    public static AbstractInsnNode getNextInstruction(AbstractInsnNode instruction) {
        if (instruction == null) return null;
        instruction = instruction.getNext();
        while (instruction instanceof LineNumberNode || instruction instanceof LabelNode || instruction instanceof FrameNode) {
            instruction = instruction.getNext();
        }
        return instruction;
    }
}
