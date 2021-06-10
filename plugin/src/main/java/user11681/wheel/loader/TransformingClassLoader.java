package user11681.wheel.loader;

import java.net.URL;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.gradle.internal.classloader.VisitableURLClassLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import user11681.reflect.Classes;
import user11681.uncheck.Uncheck;

public class TransformingClassLoader extends VisitableURLClassLoader {
    public static final long klass = Classes.klass(TransformingClassLoader.class);

    private static final List<Map.Entry<String, Transformer>> transformers = new ArrayList<>();

    public TransformingClassLoader(String name, ClassLoader parent, Collection<URL> urls) {
        super(name, parent, urls);
    }

    public static void addTransformer(String name, Transformer transformer) {
        transformers.add(Map.entry(name, transformer));
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> loadedClass = this.findLoadedClass(name);

        if (loadedClass != null) {
            return loadedClass;
        }

        if (name.startsWith("java.")) {
            return super.loadClass(name, resolve);
        }

        List<Transformer> transformerArray = new ArrayList<>();

        for (Map.Entry<String, Transformer> entry : transformers) {
            if (name.equals(entry.getKey())) {
                transformerArray.add(entry.getValue());
            }
        }

        if (transformerArray.isEmpty()) {
            return super.loadClass(name, resolve);
        }

        return Uncheck.handle(() -> {
            URL classFile = this.getResource(name.replace('.', '/') + ".class");
            ClassNode node = new ClassNode();
            new ClassReader(classFile.openStream()).accept(node, 0);

            for (Transformer transformer : transformerArray) {
                transformer.transform(node);
            }

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            node.accept(writer);
            byte[] bytecode = writer.toByteArray();
            Class<?> type = this.defineClass(name, bytecode, 0, bytecode.length, new CodeSource(classFile, (Certificate[]) null));

            if (resolve) {
                this.resolveClass(type);
            }

            return type;
        });
    }
}
