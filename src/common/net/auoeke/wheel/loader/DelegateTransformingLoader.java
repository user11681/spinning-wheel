package net.auoeke.wheel.loader;

import java.util.Arrays;

public class DelegateTransformingLoader extends TransformingClassLoader {
    public DelegateTransformingLoader(TransformingClassLoader parent) {
        super(parent.getName() + "-delegate", parent, Arrays.asList(parent.getURLs()));
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return ((TransformingClassLoader) this.getParent()).loadClass(name, resolve);
    }
}
