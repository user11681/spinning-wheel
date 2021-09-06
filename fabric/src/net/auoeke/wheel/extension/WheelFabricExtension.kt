package net.auoeke.wheel.extension

import net.auoeke.extensions.string
import org.gradle.api.Project

class WheelFabricExtension(project: Project?) : WheelExtension(project!!), WheelLoomExtensionBase {
    override var yarn: String? = null
    override var genSources: String? = "genSources"
    var channel: Channel = Channel.RELEASE
        set(value) {field = Channel.valueOf(value.string.uppercase())}
    var nospam = true
}
