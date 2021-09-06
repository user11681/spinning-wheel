package net.auoeke.wheel.extension.dependency

import net.auoeke.extensions.string
import java.util.stream.Collectors

class Repository(val key: String, val url: String) {
    val dependencies: MutableList<Dependency> = ArrayList()

    fun dependency(key: String, module: String): Repository  {
        this.dependencies.add(Dependency(key, "$module:latest.integration", this.key))

        return this
    }

    fun dependency(key: String , artifact: String , version: String ): Repository  {
        this.dependencies.add(Dependency(key, "$artifact:$version", this.key))

        return this
    }

    override fun toString(): String {
        val multiline = this.dependencies.size > 1
        val string = StringBuilder(this.key).append(": {")

        if (multiline) {
            string.append("\n\t")
        }

        string.append(this.dependencies.stream().map(Dependency::toString).collect(Collectors.joining("\n\t")))

        if (multiline) {
            string.append('\n')
        }

        return string.append('}').string
    }

    override fun equals(other: Any?): Boolean {
        return this === other || other is Repository && this.key == other.key
    }

    override fun hashCode(): Int {
        return this.key.hashCode()
    }
}
