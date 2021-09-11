package net.auoeke.wheel

import org.junit.jupiter.api.Test
import org.junit.platform.commons.annotation.Testable

@Testable
class FabricTest : WheelTest() {
    @Test
    fun test() {
        super.debug().build()
    }

    @Test
    fun publish() {
        this.debug("publish").build()
    }
}
