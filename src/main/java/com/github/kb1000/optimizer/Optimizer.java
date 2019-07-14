package com.github.kb1000.optimizer;

import org.objectweb.asm.tree.ClassNode;

public interface Optimizer {
    ClassNode transform(ClassNode classNode);
    public static final boolean DEBUG = true;
    public static final boolean INSTANT_LOAD_DEBUG = DEBUG && false;
}
