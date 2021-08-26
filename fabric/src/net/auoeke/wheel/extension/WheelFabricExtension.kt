package net.auoeke.wheel.extension

import org.gradle.api.Project

class WheelFabricExtension(project: Project?) : WheelExtension(project!!), WheelLoomExtensionBase {
    override var yarn: String? = null
    override var genSources: String? = "genSources"
    var channel: Channel = Channel.RELEASE
        set(value) {field = Channel.valueOf(value.toString().uppercase())}
    var nospam = true

    fun setChannel(channel: Any) {

    }
}
