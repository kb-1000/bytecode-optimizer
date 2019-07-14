package com.github.kb1000.optimizer.optimizers;

import java.util.ListIterator;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import com.github.kb1000.optimizer.Optimizer;

import static org.objectweb.asm.Opcodes.*;

// TODO(kb1000): This currently does NOT remove instructions in a goto-loop that is never executed. Will have to analyze which labels could be executed to remove them too.
public final class RemoveJumpedInstructions implements Optimizer {
    @Override
    public ClassNode transform(final ClassNode classNode) {
        for (final MethodNode methodNode: classNode.methods) {
            final ListIterator<AbstractInsnNode> iterator = methodNode.instructions.iterator();
            if (classNode.name.equals("com/github/kb1000/jypy/parser/Tokenizer") && methodNode.name.equals("tok_get")) {
                System.out.print("");
            }
            while (iterator.hasNext()) {
                final AbstractInsnNode instruction = iterator.next();
                if (instruction.getOpcode() == GOTO && iterator.hasNext()) {
                    // this loop should never be executed if the next item is already a label
                    AbstractInsnNode nextInstruction = iterator.next();
                    int i = 0;
                    while (!(nextInstruction instanceof LabelNode) && iterator.hasNext()) {
                        iterator.remove();
                        if (Optimizer.DEBUG) {
                            i++;
                        }
                        if (iterator.hasNext()) nextInstruction = iterator.next();
                    }
                    if (Optimizer.DEBUG && i != 0) {
                        System.err.println("Removed " + i + " instructions jumped over in " + classNode.name + "->" + methodNode.name);
                    }
                }
            }
        }
        return classNode;
    }
}
