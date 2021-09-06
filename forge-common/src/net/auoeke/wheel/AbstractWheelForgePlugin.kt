package net.auoeke.wheel

import net.auoeke.wheel.extension.WheelExtension
import net.auoeke.wheel.extension.WheelForgeExtensionBase
import org.w3c.dom.Node
import java.util.*
import java.util.stream.Stream
import javax.xml.parsers.DocumentBuilderFactory

abstract class AbstractWheelForgePlugin<P : AbstractWheelForgePlugin<P, E>, E> : WheelPlugin<P, E>() where E : WheelExtension, E : WheelForgeExtensionBase {
    override val metadataFile: String get() = "mods.toml"

    override fun checkMinecraftVersion() {
        var versioning: Node? = null

        if (this.extension.minecraft === null) {
            versioning = versioning()

            if (latestMinecraftVersion === null) {
                if (this.extension.forge === null) {
                    val versions = versioning.childNodes.item(1).textContent.split("-", limit = 2) as MutableList
                    latestMinecraftVersion = versions[0]
                    this.extension.minecraft = latestMinecraftVersion
                    this.extension.forge = versions[1]
                } else {
                    this.extension.minecraft = nodeStream(versioning.lastChild.firstChild)
                        .filter {version -> version.endsWith(this.extension.forge!!)}
                        .findFirst()
                        .map {version -> version.substring(0, version.indexOf('-'))}
                        .orElseThrow {IllegalArgumentException("""Forge version "${this.extension.forge}" was not found at $FORGE_METADATA_URL.""")}
                }
            } else {
                this.extension.minecraft = latestMinecraftVersion
            }
        }

        if (this.extension.forge === null) {
            this.extension.forge = nodeStream((versioning ?: versioning()).lastChild.firstChild)
                .filter {version -> version.startsWith(this.extension.minecraft!!)}
                .findFirst()
                .map {version: String -> version.substring(version.indexOf('-') + 1)}
                .orElseThrow {IllegalArgumentException("""Minecraft version "${this.extension.minecraft}" was not found at $FORGE_METADATA_URL.""")}
        }

        this.log("Forge version: ${this.extension.forge}")
    }

    fun defaultJavaVersion(): String {
        return if (this.extension.minecraft!!.split("\\.", limit = 3).toMutableList()[1].toInt() >= 17) "16" else "8"
    }

    companion object {
        protected const val FORGE_METADATA_URL = "https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml"

        protected fun versioning(): Node = DocumentBuilderFactory
           .newInstance()
           .newDocumentBuilder()
           .parse(this.FORGE_METADATA_URL)
           .firstChild
           .lastChild

        protected fun nodeStream(first: Node): Stream<String> = Stream.iterate(first, Objects::nonNull, Node::getNextSibling).map(Node::getTextContent)
    }
}
