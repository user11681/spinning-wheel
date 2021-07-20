package user11681.wheel.util;

import java.util.function.Supplier;

public class Lazy<T> {
    private final Supplier<T> initializer;

    @SuppressWarnings("unchecked")
    private T object = (T) this;

    private Lazy(Supplier<T> initializer) {
        this.initializer = initializer;
    }

    public static <T> Lazy<T> of(Supplier<T> initializer) {
        return new Lazy<>(initializer);
    }

    public T get() {
        return this.object == this ? this.object = this.initializer.get() : this.object;
    }
}
