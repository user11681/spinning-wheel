package user11681.wheel.util;

import java.io.PrintStream;
import org.gradle.internal.io.LinePerThreadBufferingOutputStream;
import org.gradle.internal.io.TextStream;
import org.jetbrains.annotations.NotNull;

public class FilteredPrintStream extends LinePerThreadBufferingOutputStream {
    private FilteredPrintStream(TextStream handler) {
        super(handler);
    }

    @Override
    public PrintStream printf(@NotNull String format, Object... args) {
        return format.startsWith("unknown invokedynamic bsm:") ? this : super.printf(format, args);
    }
}
