package net.auoeke.wheel.util

import java.util.*

fun sanitize(key: String): String = key.replace("[_-]".toRegex(), "").lowercase(Locale.ROOT)

fun <R, F : () -> R> F.tryCatch(fallback: () -> R): R = try {
    this()
} catch (ignored: Throwable) {
    fallback()
}
