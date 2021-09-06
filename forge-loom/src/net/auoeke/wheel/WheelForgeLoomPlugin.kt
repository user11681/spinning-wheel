package net.auoeke.wheel

import net.auoeke.wheel.extension.WheelForgeLoomExtension
import net.fabricmc.loom.util.Constants
import org.gradle.api.Project

class WheelForgeLoomPlugin : AbstractWheelForgePlugin<WheelForgeLoomPlugin, WheelForgeLoomExtension>(), WheelLoomPlugin<WheelForgeLoomExtension> {
    override fun apply(project: Project) {
        super<AbstractWheelForgePlugin>.apply(project, "dev.architectury.loom", WheelForgeLoomExtension(project))
    }

    override fun beforeMain() {
        this.extra["loom.platform"] = "forge"
        this.extra["loom.forge.include"] = "true"
    }

    override fun addRepositories() {
        super.addRepositories()

        this.repository("https://maven.architectury.dev")
    }

    override fun checkMinecraftVersion() {
        super<AbstractWheelForgePlugin>.checkMinecraftVersion()
        super<WheelLoomPlugin>.checkMinecraftVersion()
    }

    override fun addDependencies() {
        super<AbstractWheelForgePlugin>.addDependencies()

        this.dependency(Constants.Configurations.MINECRAFT, "com.mojang:minecraft:${this.extension.minecraft}")
        this.dependency(Constants.Configurations.MAPPINGS, "net.fabricmc:yarn:${this.extension.minecraft}+build.${this.extension.yarn}:v2")
        this.dependency(Constants.Configurations.FORGE, "net.minecraftforge:forge:${this.extension.minecraft}-${this.extension.forge}")
    }
}
