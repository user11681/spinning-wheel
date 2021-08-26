package net.auoeke.wheel.extension.dependency

import net.auoeke.wheel.extension.WheelExtension
import java.util.*

class Dependency(val key: String, val artifact: String, val repository: String) {
    override fun equals(other: Any?): Boolean {
        return other === this || other is Dependency
            && key == other.key
            && artifact == other.artifact
            && repository == other.repository
    }

    override fun hashCode(): Int = Objects.hash(key, artifact, repository)
    override fun toString(): String = "$key ($artifact)"

    fun resolveRepository(): String = WheelExtension.repository(repository)!!
}
