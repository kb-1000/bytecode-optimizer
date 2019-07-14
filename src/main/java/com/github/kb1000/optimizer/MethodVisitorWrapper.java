package com.github.kb1000.optimizer;

import java.lang.invoke.MethodHandle;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

public final class MethodVisitorWrapper extends ClassVisitor {
    private final MethodHandle constructor;

    public MethodVisitorWrapper(final ClassVisitor cv, final MethodHandle constructor) {
        super(ASM7, cv);
        this.constructor = constructor;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
        final MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (mv != null) {
            try {
                return (MethodVisitor) constructor.invoke(mv, access, name, descriptor, signature, exceptions);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        } else {
            return null;
        }
    }
}
