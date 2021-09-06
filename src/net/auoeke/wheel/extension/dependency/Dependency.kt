package net.auoeke.wheel.extension.dependency

import net.auoeke.wheel.extension.WheelExtension
import java.util.*

class Dependency(val key: String, val artifact: String, val repository: String) {
    override fun equals(other: Any?): Boolean {
        return other === this || other is Dependency
            && this.key == other.key
            && this.artifact == other.artifact
            && this.repository == other.repository
    }

    override fun hashCode(): Int = Objects.hash(this.key, this.artifact, this.repository)
    override fun toString(): String = "${this.key} (${this.artifact})"

    fun resolveRepository(): String = WheelExtension.repository(this.repository)!!
}
