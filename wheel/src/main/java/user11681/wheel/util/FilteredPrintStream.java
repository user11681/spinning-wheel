package user11681.wheel.util;

import java.io.PrintStream;
import org.jetbrains.annotations.NotNull;

public class FilteredPrintStream extends PrintStream {
    public FilteredPrintStream(PrintStream parent) {
        super(parent);
    }

    @Override
    public PrintStream printf(@NotNull String format, Object... args) {
        return format.startsWith("unknown invokedynamic bsm:") ? this : super.printf(format, args);
    }
}
