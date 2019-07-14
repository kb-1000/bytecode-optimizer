package com.github.kb1000.optimizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * This is just a workaround for a different problem, namely that the
 * optimizers that remove instructions can't compensate for frame or debug
 * information, but since frames have to exist, they have to be recomputed,
 * which in turn would require ASM in its current implementation to load the
 * class into runtime and thus "infect" the process with the optimized bytecode.
 * This replaces that implementation by a pluggable implementation, which is
 * used to implement that based on bytecode instead of runtime reflection.
 * But, it will not be removed when the optimizers can handle frames, since ASM
 * may change its implementation to depend on this anyways at any time.
 */
public abstract class ClassWriterFix extends ClassWriter {
    public ClassWriterFix(final ClassReader classReader, final int flags) {
        super(classReader, flags);
    }

    public ClassWriterFix(final int flags) {
        this(null, flags);
    }

    @Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        ClassData class1 = fetchClassData(type1);
        final ClassData class2 = fetchClassData(type2);
        if (class1.isAssignableFrom(this, class2)) return type1;
        if (class2.isAssignableFrom(this, class1)) return type2;
        if (class1.isInterface() || class2.isInterface()) return "java/lang/Object";
        else {
            do {
                class1 = fetchClassData(class1.getSuperType());
            } while (!class1.isAssignableFrom(this, class2));
            return class1.getName().replace('.', '/');
        }
    }

    protected abstract ClassData fetchClassData(final String type);

    public interface ClassData {
        String getSuperType();
        boolean isInterface();
        String getName();
        List<String> getSuperInterfaces();
        default boolean isAssignableFrom(final ClassWriterFix classWriter, final ClassData type2) {
            if (getName().equals("java/lang/Object")) return true;
            if (type2.getName().equals("java/lang/Object")) return false;
            if (getName().equals(type2.getName())) {
                return true;
            }

            if (type2.getName().equals(getName())) {
                return true;
            }

            if (isAssignableFrom(classWriter, classWriter.fetchClassData(type2.getSuperType()))) {
                return true;
            }

            for (final String interface_: type2.getSuperInterfaces()) {
                if (isAssignableFrom(classWriter, classWriter.fetchClassData(interface_))) {
                    return true;
                }
            }

            return false;
        }
    }

    public static final class RegisterClassDataClassWriterFix extends ClassWriterFix {
        private static final FinalHashMap<String, RegisteredClassData> map = new FinalHashMap<>();

        public RegisterClassDataClassWriterFix(final ClassReader classReader, final int flags) {
            super(classReader, flags);
        }

        public RegisterClassDataClassWriterFix(final int flags) {
            this(null, flags);
        }

        @Override
        protected ClassData fetchClassData(final String type) {
            return Optional.ofNullable(map.get(type)).orElseThrow(() -> new TypeNotPresentException(type, null));
        }

        private static final class RegisteredClassData implements ClassData {
            private final String name;
            private final String superType;
            private final boolean isInterface;
            private final List<String> superInterfaces;

            private RegisteredClassData(final String name, final String superType, final boolean isInterface, final List<String> superInterfaces) {
                this.name = name;
                this.superType = superType;
                this.isInterface = isInterface;
                this.superInterfaces = Collections.unmodifiableList(new ArrayList<>(superInterfaces));
            }

            @Override
            public String getSuperType() {
                return superType;
            }

            @Override
            public boolean isInterface() {
                return isInterface;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public List<String> getSuperInterfaces() {
                return superInterfaces;
            }
        }

        public static void register(final String name, final String superType, final boolean isInterface, final List<String> superInterfaces) {
            map.put(name, new RegisteredClassData(name, superType, isInterface, superInterfaces));
        }
    }
}
