package net.auoeke.wheel.extension

import org.gradle.api.Project

class WheelForgeLoomExtension(project: Project) : WheelExtension(project), WheelForgeExtensionBase, WheelLoomExtensionBase {
    override var yarn: String? = null
    override var forge: String? = null
}
