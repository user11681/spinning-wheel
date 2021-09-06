package net.auoeke.wheel

import org.junit.jupiter.api.Test
import org.junit.platform.commons.annotation.Testable

@Testable
class ForgeTest : WheelTest() {
    @Test
    fun run() {
        super.run().build()
    }
}
