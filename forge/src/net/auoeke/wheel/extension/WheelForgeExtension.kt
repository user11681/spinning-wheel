package net.auoeke.wheel.extension

import org.gradle.api.Project

class WheelForgeExtension(project: Project) : WheelExtension(project), WheelForgeExtensionBase {
    override var forge: String? = null
}
