package net.auoeke.wheel.loader;

import java.net.URL;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
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
    private static final Map<TransformingClassLoader, DelegateTransformingLoader> delegates = new IdentityHashMap<>();

    public TransformingClassLoader(String name, ClassLoader parent, Collection<URL> urls) {
        super(name, parent, urls);
    }

    public void transform(String name, Transformer transformer) {
        Class<?> foundClass = this.findLoadedClass(name);

        if (foundClass == null) {
            transformers.add(Map.entry(name, transformer));
        } else {
            delegates.computeIfAbsent(this, DelegateTransformingLoader::new).define(name, false, transformer);
        }
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!name.startsWith("java.")) {
            DelegateTransformingLoader delegate = delegates.get(this);

            if (delegate != null) {
                Class<?> override = delegate.findLoadedClass(name);

                if (override != null) {
                    return override;
                }
            }

            for (int index = transformers.size() - 1; index >= 0; index--) {
                Map.Entry<String, Transformer> entry = transformers.get(index);

                if (name.equals(entry.getKey())) {
                    transformers.remove(index);

                    return this.define(name, resolve, entry.getValue());
                }
            }
        }

        return super.loadClass(name, resolve);
    }

    @SuppressWarnings("ConstantConditions")
    protected Class<?> define(String name, boolean resolve, Transformer transformer) {
        URL classFile = this.getResource(name.replace('.', '/') + ".class");
        ClassNode node = new ClassNode();
        Uncheck.handle(() -> new ClassReader(classFile.openStream()).accept(node, 0));
        transformer.transform(node);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        node.accept(writer);
        byte[] bytecode = writer.toByteArray();
        Class<?> type = this.defineClass(name, bytecode, 0, bytecode.length, new CodeSource(classFile, (Certificate[]) null));

        if (resolve) {
            this.resolveClass(type);
        }

        return type;
    }
}
