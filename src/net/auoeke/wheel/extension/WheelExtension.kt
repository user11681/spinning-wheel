package net.auoeke.wheel.extension

import groovy.lang.Closure
import net.auoeke.wheel.dependency.RepositoryContainer
import net.auoeke.wheel.extension.dependency.Dependency
import net.auoeke.wheel.extension.publish.PublishingConfig
import org.gradle.api.Project

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
abstract class WheelExtension(val project: Project) {
    var clean = true
    var minecraft: String? = null
    var java = JavaVersions()
    var publish = PublishingConfig(this.project)
    var run = RunDirectory()

    fun publish(action: Closure<*>?) {
        this.project.configure(this.publish, action)
    }

    fun setPublish(enabled: Boolean) {
        this.publish.enabled = enabled
    }

    fun java(action: Closure<*>?) {
        this.project.configure(this.java, action)
    }

    fun setJava(version: Any?) {
        this.java.source = version
        this.java.target = version
    }

    companion object {
        private val repositories: RepositoryContainer = RepositoryContainer().configure {
            repository("auoeke", "https://maven.auoeke.net") {
                dependency("bason", "user11681:bason")
                dependency("cell", "user11681:cell")
                dependency("common-formatting", "user11681:common-formatting")
                dependency("dynamic-entry", "user11681:dynamicentry")
                dependency("gross-fabric-hacks", "net.devtech:grossfabrichacks")
                dependency("huntingham-hills", "user11681:huntingham-hills")
                dependency("invisible-living-entities", "user11681:invisiblelivingentities")
                dependency("javac-plugin", "user11681:javac-plugin")
                dependency("liberica", "user11681:liberica")
                dependency("limitless", "net.auoeke:limitless")
                dependency("narrator-off", "user11681:narrator-off")
                dependency("noauth", "user11681:noauth")
                dependency("numerus-romani", "user11681:numerus-romani")
                dependency("optional", "user11681:optional")
                dependency("phormat", "user11681:phormat")
                dependency("project-fabrok", "user11681:projectfabrok")
                dependency("prone", "user11681:prone")
                dependency("reflect", "net.auoeke:reflect")
                dependency("safe", "net.auoeke:safe")
                dependency("shortcode", "user11681:shortcode")
                dependency("uncheck", "user11681:uncheck")
                dependency("unsafe", "net.gudenau.lib:unsafe")
            }

            repository("blamejared", "https://maven.blamejared.com")

            repository("boundarybreaker", "https://server.bbkr.space/artifactory/libs-release") {
                dependency("cotton-resources", "io.github.cottonmc:cotton-resources")
            }

            repository("buildcraft", "https://mod-buildcraft.com/maven")

            repository("central", "https://repo.maven.apache.org/maven2/") {
                dependency("jabel", "com.github.bsideup.jabel:jabel-javac-plugin")
                dependency("junit", "org.junit.jupiter:junit-jupiter")
                dependency("toml4j", "com.moandjiezana.toml:toml4j")
            }

            repository("cursemaven", "https://www.cursemaven.com") {
                dependency("aquarius", "curse.maven:aquarius-301299", "3132504")
                dependency("charm", "curse.maven:charm-318872", "3140951")
                dependency("moenchantments", "curse.maven:moenchantments-320806", "3084973")
                dependency("better-slabs", "curse.maven:betterslabs-407645", "3158336")
            }

            repository("dblsaiko", "https://maven.dblsaiko.net/")

            repository("earthcomputer", "https://dl.bintray.com/earthcomputer/mods") {
                dependency("multiconnect", "net.earthcomputer:multiconnect:api")
            }

            repository("fabric", "https://maven.fabricmc.net") {
                dependency("api", "net.fabricmc.fabric-api:fabric-api")
                dependency("api-base", "net.fabricmc.fabric-api:fabric-api-base")
                dependency("api-block-render-layer", "net.fabricmc.fabric-api:fabric-blockrenderlayer-v1")
                dependency("api-command", "net.fabricmc.fabric-api:fabric-command-api-v1")
                dependency("api-events-interaction", "net.fabricmc.fabric-api:fabric-events-interaction-v0")
                dependency("api-key-bindings", "net.fabricmc.fabric-api:fabric-key-binding-api-v1")
                dependency("api-lifecycle-events", "net.fabricmc.fabric-api:fabric-lifecycle-events-v1")
                dependency("api-networking", "net.fabricmc.fabric-api:fabric-networking-api-v1")
                dependency("api-registry-sync", "net.fabricmc.fabric-api:fabric-registry-sync-v0")
                dependency("api-renderer-api", "net.fabricmc.fabric-api:fabric-renderer-api-v1")
                dependency("api-renderer-indigo", "net.fabricmc.fabric-api:fabric-renderer-indigo")
                dependency("api-resource-loader", "net.fabricmc.fabric-api:fabric-resource-loader-v0")
                dependency("api-screen", "net.fabricmc.fabric-api:fabric-screen-api-v1")
                dependency("api-screen-handler", "net.fabricmc.fabric-api:fabric-screen-handler-api-v1")
                dependency("api-tag-extensions", "net.fabricmc.fabric-api:fabric-tag-extensions-v0")
            }

            repository("gross-fabric-hackers", "https://raw.githubusercontent.com/GrossFabricHackers/maven/master")

            repository("halfof2", "https://storage.googleapis.com/devan-maven") {
                dependency("arrp", "net.devtech:arrp")
            }

            repository("haven-king", "https://hephaestus.dev/release") {
                dependency("conrad", "dev.inkwell:conrad")
            }

            repository("jamies-white-shirt", "https://maven.jamieswhiteshirt.com/libs-release") {
                dependency("reach-entity-attributes", "com.jamieswhiteshirt:reach-entity-attributes")
            }

            repository("jitpack", "https://jitpack.io") {
                dependency("astromine", "com.github.Chainmail-Studios:Astromine", "1.8.1")
                dependency("fabric-asm", "com.github.Chocohead:Fabric-ASM", "master-SNAPSHOT")
                dependency("lil-tater-reloaded", "com.github.Yoghurt4C:LilTaterReloaded", "fabric-1.16-SNAPSHOT")
                dependency("starlight", "com.github.Tuinity:Starlight", "fabric-SNAPSHOT")
            }

            repository("ladysnake", "https://ladysnake.jfrog.io/artifactory/mods") {
                dependency("cardinal-components", "io.github.onyxstudios:Cardinal-Components-API")
                dependency("cardinal-components-base", "io.github.onyxstudios.Cardinal-Components-API:cardinal-components-base")
                dependency("cardinal-components-entity", "io.github.onyxstudios.Cardinal-Components-API:cardinal-components-entity")
                dependency("cardinal-components-item", "io.github.onyxstudios.Cardinal-Components-API:cardinal-components-item")
            }

            repository("shedaniel", "https://maven.shedaniel.me") {
                dependency("auto-config", "me.sargunvohra.mcmods:autoconfig1u")
                dependency("basic-math", "me.shedaniel.cloth:basic-math")
                dependency("cloth-config", "me.shedaniel.cloth:cloth-config-fabric")
                dependency("rei", "me.shedaniel:RoughlyEnoughItems")
            }

            repository("terraformers", "https://maven.terraformersmc.com") {
                dependency("modmenu", "com.terraformersmc:modmenu")
            }

            repository("wrenchable", "https://dl.bintray.com/zundrel/wrenchable")
        }

        fun repository(key: String): String? = this.repositories.repository(key)
        fun repository(key: String, value: String) = this.repositories.putRepository(key, value)

        fun dependency(key: String): Dependency? = this.repositories.entry(key)
    }
}
