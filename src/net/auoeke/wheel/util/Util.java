package net.auoeke.wheel.util;

import java.util.Locale;

public class Util {
    public static String sanitize(String key) {
        return key.replaceAll("[_-]", "").toLowerCase(Locale.ROOT);
    }
}
