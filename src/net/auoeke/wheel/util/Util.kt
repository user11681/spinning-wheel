package net.auoeke.wheel.util

import java.util.*
import java.util.function.Supplier

object Util {
    fun sanitize(key: String): String {
        return key.replace("[_-]".toRegex(), "").lowercase(Locale.ROOT)
    }

    fun <T> tryCatch(main: Supplier<T>, fallback: Supplier<T>): T {
        return try {
            main.get()
        } catch (ignored: Throwable) {
            fallback.get()
        }
    }
}
