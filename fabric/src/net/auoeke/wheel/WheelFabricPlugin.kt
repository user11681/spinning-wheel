package net.auoeke.wheel

import net.auoeke.wheel.extension.Channel
import net.auoeke.wheel.extension.WheelFabricExtension
import net.fabricmc.loom.util.Constants
import org.gradle.api.Project

class WheelFabricPlugin : WheelPlugin<WheelFabricPlugin, WheelFabricExtension>(), WheelLoomPlugin<WheelFabricExtension> {
    override val metadataFile: String = "fabric.mod.json"
    override val defaultJavaVersion: String = "16"

    override fun apply(project: Project) {
        super<WheelPlugin>.apply(project, "fabric-loom", WheelFabricExtension(project))
    }

    override fun checkMinecraftVersion() {
        if (this.extension.minecraft === null) {
            if (latestMinecraftVersion === null) {
                latestMinecraftVersion = this.meta(
                    "game",
                    if (this.extension.channel == Channel.RELEASE) "(?<=\"version\": \").*?(?=\",\\s*\"stable\": true)" else "(?<=\"version\": \").*?(?=\")"
                ).filter {!it.group().contains("experiment")}.findFirst().orElseThrow().group()
            }

            this.extension.minecraft = latestMinecraftVersion
        }

        super.checkMinecraftVersion()
    }

    override fun addDependencies() {
        super<WheelPlugin>.addDependencies()

        this.dependency(Constants.Configurations.MINECRAFT, "com.mojang:minecraft:${this.extension.minecraft}")
        this.dependency(Constants.Configurations.MAPPINGS, "net.fabricmc:yarn:${this.extension.minecraft}+build.${this.extension.yarn}:v2")
        this.dependency(WheelPluginBase.MOD, "net.fabricmc:fabric-loader:latest.integration")

        if (this.extension.nospam) {
            this.dependency(WheelPluginBase.MOD, "narrator-off")
            this.dependency(WheelPluginBase.MOD, "noauth")
        }
    }
}
