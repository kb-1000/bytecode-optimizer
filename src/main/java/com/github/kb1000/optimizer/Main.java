package com.github.kb1000.optimizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.TraceClassVisitor;

import com.github.kb1000.optimizer.optimizers.*;

public final class Main {
    public static void main(String... argv) throws Throwable {
        if (false) {
            System.out.println(org.objectweb.asm.util.Textifier.class);
        }
        final Options options = new Options();
        {
            final Iterator<String> iterator = Arrays.asList(argv).iterator();
            while (iterator.hasNext()) {
                final String opt = iterator.next();
                if (opt.charAt(0) == '-') {
                    if (opt.equals("-o")) {
                        final String output = iterator.next();
                        if (options.output != null) {
                            System.err.println("-o was already specified.");
                            System.exit(1);
                        }
                        options.output = output;
                    } else if (opt.equals("-cp")) {
                        options.libraries.add(iterator.next());
                    } else {
                        System.err.println("Unknown option: " + opt);
                    }
                } else {
                    options.program.add(opt);
                }
            }
        }
        final File output = new File(options.output);
        final Optimizer[] optimizers;
        final MethodHandle jsrInlinerConstructor, localVariablesSorterConstructor;
        {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            final MethodType type = MethodType.methodType(void.class);
            final MethodType jsrInlinerType = MethodType.methodType(void.class, MethodVisitor.class, int.class, String.class, String.class, String.class, String[].class);
            final MethodType localVariablesSorterType = MethodType.methodType(void.class, int.class, String.class, MethodVisitor.class);
            jsrInlinerConstructor = lookup.findConstructor(JSRInlinerAdapter.class, jsrInlinerType);
            localVariablesSorterConstructor = MethodHandles.permuteArguments(lookup.findConstructor(LocalVariablesSorter.class, localVariablesSorterType), jsrInlinerType.changeReturnType(LocalVariablesSorter.class), 1, 3, 0);
            @SuppressWarnings("unchecked")
            final Class<? extends Optimizer>[] classes = (Class<? extends Optimizer>[]) new Class<?>[] { RemoveNOPs.class, ForwardDoubleJumps.class, ForwardIfAfterJump.class, RemoveJumpedInstructions.class, RemoveJumpToFollowingInstruction.class, RemoveNOPs.class, ForwardConstantIf.class, RemoveInstructionsAfterReturn.class, TransformInstantLoads.class, RemoveUnloadedStores.class, RemoveConstantPOPs.class, RevertInstantLoads.class, RemoveLoadStore.class };
            final int count = classes.length;
            optimizers = new Optimizer[count];
            for (int i = 0; i < count; i++) {
                final Class<? extends Optimizer> clazz = classes[i];
                optimizers[i] = (Optimizer) (lookup.findConstructor(clazz, type).invoke());
            }
        }

        for (final String file: options.libraries) {
            loadFile(file);
        }

        for (final String file: options.program) {
            loadFile(file);
        }

        for (final String arg: options.program) {
            ClassNode classNode = new ClassNode();
            try (final InputStream file = new FileInputStream(arg)) {
                final ClassReader reader = new ClassReader(file);
                reader.accept(new MethodVisitorWrapper(classNode, jsrInlinerConstructor), ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
            for (final Optimizer optimizer: optimizers) {
                // Implementers are allowed to return the same ClassNode
                classNode = optimizer.transform(classNode);
            }
            System.out.println(classNode.name);
            // If we passed the ClassReader in here, it might get a lot of unneeded constants in theory, if we removed all references to them
            final ClassWriter writer = new ClassWriterFix.RegisterClassDataClassWriterFix(ClassWriter.COMPUTE_FRAMES);
            for (final MethodNode methodNode: classNode.methods) {
                methodNode.instructions.resetLabels();
            }
            try (final FileOutputStream fos = new FileOutputStream("t.txt")) {
                classNode.accept(new MethodVisitorWrapper(new TraceClassVisitor(new PrintWriter(fos)), localVariablesSorterConstructor));
            }
            classNode.accept(new MethodVisitorWrapper(writer, localVariablesSorterConstructor));
            final File f = new File(output, classNode.name + ".class");
            f.getParentFile().mkdirs();
            try (final OutputStream outStream = new FileOutputStream(f)) {
                outStream.write(writer.toByteArray());
            }
        }
    }

    private static void loadFile(final String file) throws IOException {
        if (file.endsWith(".class")) {
            try (final InputStream is = new FileInputStream(file)) {
                loadFile(is);
            }
        } else if (file.endsWith(".jar") || file.endsWith(".zip") || file.endsWith(".jmod")) {
            try (final InputStream is = new FileInputStream(file)) {
                if (file.endsWith(".jmod")) {
                    is.read(new byte[4]); // skip magic number
                }
                try (final ZipInputStream zis = new ZipInputStream(is)) {
                    while (true) {
                        final ZipEntry entry = zis.getNextEntry();
                        if (entry != null) {
                            if (entry.getName().endsWith(".class")) {
                                loadFile(zis);
                            }
                        } else {
                            break;
                        }
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Can't handle " + file);
        }
    }

    private static void loadFile(final InputStream is) throws IOException {
        final ClassReader classReader = new ClassReader(is);
        classReader.accept(new ClassVisitor(Opcodes.ASM7) {
            @Override
            public void visit(final int version, final int access, final String name, final String signature, final String superClass, final String[] interfaces) {
                ClassWriterFix.RegisterClassDataClassWriterFix.register(name, superClass, (access | Opcodes.ACC_INTERFACE) != 0, Arrays.asList(interfaces));
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }
}
