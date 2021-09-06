package net.auoeke.wheel

import net.auoeke.extensions.type
import net.auoeke.reflect.Invoker
import net.auoeke.wheel.extension.WheelExtension
import net.auoeke.wheel.extension.WheelLoomExtensionBase
import net.fabricmc.loom.LoomGradleExtension
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.configuration.ide.RunConfigSettings
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.publish.maven.MavenPublication
import org.intellij.lang.annotations.Language
import java.io.File
import java.nio.file.Path
import java.util.regex.MatchResult
import java.util.stream.Stream
import kotlin.io.path.*

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
interface WheelLoomPlugin<E> : WheelPluginBase<E> where E : WheelExtension, E : WheelLoomExtensionBase {
    val loom: LoomGradleExtension get() = this.extension(LoomGradleExtensionAPI::class) as LoomGradleExtension
    val cache: Path get() = this.loom.files.userCache.toPath()
    val runConfigs: NamedDomainObjectContainer<RunConfigSettings> get() = this.loom.runConfigs

    private val sourcePath: File get() = Invoker.bind(this.task(this.extension.genSources!!), "getMappedJarFileWithSuffix", type<File>(), type<String>())("-sources.jar") as File

    override fun afterEvaluation() {
        super.afterEvaluation()

        val test = this.sourceSet("test")

        this.runConfigs.create("testClient") {settings ->
            settings.client()
            settings.source(test)
        }

        this.runConfigs.create("testServer") {settings ->
            settings.server()
            settings.source(test)
        }

        this.runConfigs.all {it.isIdeConfigGenerated = true}
    }

    override fun configureConfigurations() {
        super.configureConfigurations()

        this.configuration("modApi").extendsFrom(this.configuration(WheelPluginBase.MOD))
    }

    override fun configurePublication(publication: MavenPublication) {
        super.configurePublication(publication)

        publication.artifact(this.task("remapJar"))
        publication.artifact(this.task("sourcesJar")).builtBy(this.task("remapSourcesJar"))
    }

    override fun afterMain() {
        this.generateSources()
        this.setRunDirectory()
    }

    override fun checkMinecraftVersion() {
        if (this.extension.yarn === null) {
            this.extension.yarn = latestYarnBuilds.computeIfAbsent(this.extension.minecraft!!) {minecraft ->
                this.meta(
                    "yarn/$minecraft",
                    "(?<=\"build\": )\\d+"
                ).findFirst().orElseThrow().group()
            }
        }

        this.log("Yarn build: ${this.extension.yarn}")
    }

    fun meta(name: String, @Language("RegExp") pattern: String): Stream<MatchResult> {
        return pattern.toPattern().matcher(this.get("https://meta.fabricmc.net/v2/versions/$name")).results()
    }

    fun setRunDirectory() {
        if (this.isRoot && this.extension.run.enabled) {
            this.runConfigs.stream().map {it.runDir}.distinct().map {this.root.file(it).toPath()}.forEach forEach@{oldPath ->
                val customPath: String? = this.extension.run.path
                val runPath = if (customPath === null) this.cache.resolve("run") else Path(customPath)

                if (runPath.exists() && !runPath.isDirectory()) {
                    throw FileAlreadyExistsException(runPath.toFile(), reason = "exists and is not a directory")
                }

                if (oldPath.exists()) {
                    if (oldPath.isSymbolicLink()) {
                        val linkTarget: Path = oldPath.readSymbolicLink()

                        if (linkTarget.isSameFileAs(runPath)) {
                            runPath.createDirectories()

                            return@forEach
                        }

                        if (!linkTarget.exists()) {
                            oldPath.deleteExisting()
                        }
                    }

                    if (runPath.exists()) {
                        if (oldPath.isDirectory() && oldPath.listDirectoryEntries().count() == 0) {
                            oldPath.deleteExisting()
                        } else {
                            oldPath.makeOld()
                        }
                    } else if (oldPath.isDirectory()) {
                        oldPath.moveTo(runPath)
                    }
                }

                runPath.createDirectories()
                oldPath.createSymbolicLinkPointingTo(runPath)
            }
        }
    }

    private fun generateSources() {
        if (this.extension.genSources !== null && !this.sourcePath.exists()) {
            this.log("Sources not found; executing ${this.extension.genSources}.")
            this.execute(this.extension.genSources!!)
        }
    }

    companion object {
        private fun Path.makeOld() {
            val moveOld = "$this-old"
            var moveOldPath = Path(moveOld + 0)
            var i = 1

            while (moveOldPath.exists()) {
                moveOldPath = Path(moveOld + i)
                i++
            }

            this.moveTo(moveOldPath)
        }

        val latestYarnBuilds: MutableMap<String, String> = HashMap()
    }
}
