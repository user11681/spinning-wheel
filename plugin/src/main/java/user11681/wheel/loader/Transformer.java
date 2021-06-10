package user11681.wheel.loader;

import org.objectweb.asm.tree.ClassNode;

public interface Transformer {
    void transform(ClassNode type);
}
