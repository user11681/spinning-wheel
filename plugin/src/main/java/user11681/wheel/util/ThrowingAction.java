package user11681.wheel.util;

import net.gudenau.lib.unsafe.Unsafe;
import org.gradle.api.Action;

public interface ThrowingAction<T> extends Action<T> {
    void accept(T object) throws Throwable;

    @Override
    default void execute(T argument) {
        try {
            this.accept(argument);
        } catch (Throwable throwable) {
            throw Unsafe.throwException(throwable);
        }
    }
}
