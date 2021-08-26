package net.auoeke.wheel

import net.auoeke.reflect.Invoker
import net.auoeke.wheel.extension.WheelExtension
import net.auoeke.wheel.extension.WheelLoomExtensionBase
import net.fabricmc.loom.LoomGradleExtension
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.configuration.ide.RunConfigSettings
import net.fabricmc.loom.task.RunGameTask
import org.codehaus.groovy.runtime.StringGroovyMethods
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.publish.maven.MavenPublication
import org.intellij.lang.annotations.Language
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.MatchResult
import java.util.stream.Stream

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
interface WheelLoomPlugin<E> : WheelPluginBase<E> where E : WheelExtension, E : WheelLoomExtensionBase {
    val loom: LoomGradleExtension get() = extension(LoomGradleExtensionAPI::class) as LoomGradleExtension
    val cache: Path get() = loom.files.userCache.toPath()
    val runConfigs: NamedDomainObjectContainer<RunConfigSettings> get() = loom.runConfigs

    private val sourcePath: File get() = Invoker.bind(this.task(extension.genSources!!), "getMappedJarFileWithSuffix", File::class.java, String::class.java)("-sources.jar") as File

    override fun afterEvaluation() {
        super.afterEvaluation()

        val test = sourceSet("test")

        runConfigs.create("testClient") {settings ->
            settings.client()
            settings.source(test)
        }

        runConfigs.create("testServer") {settings ->
            settings.server()
            settings.source(test)
        }

        runConfigs.all {settings -> settings.isIdeConfigGenerated = true}
    }

    override fun configureConfigurations() {
        super.configureConfigurations()

        configuration("modApi").extendsFrom(configuration(WheelPluginBase.MOD))
    }

    override fun configurePublication(publication: MavenPublication) {
        super.configurePublication(publication)

        publication.artifact(this.task("remapJar"))
        publication.artifact(this.task("sourcesJar")).builtBy(this.task("remapSourcesJar"))
    }

    override fun afterMain() {
        generateSources()
        setRunDirectory()
    }

    override fun checkMinecraftVersion() {
        if (extension.yarn == null) {
            extension.yarn = latestYarnBuilds.computeIfAbsent(extension.minecraft!!) {minecraft ->
                meta(
                    "yarn/$minecraft",
                    "(?<=\"build\": )\\d+"
                ).findFirst().orElseThrow().group()
            }
        }

        log("Yarn build: ${extension.yarn}")
    }

    fun runTask(configuration: RunConfigSettings): RunGameTask? {
        return this.task("run" + StringGroovyMethods.capitalize(configuration.name))
    }

    fun meta(name: String, @Language("RegExp") pattern: String): Stream<MatchResult> {
        return pattern.toPattern().matcher(this.get("https://meta.fabricmc.net/v2/versions/$name")).results()
    }

    fun setRunDirectory() {
        if (isRoot && extension.run.enabled) {
            runConfigs.stream().map {it.runDir}.distinct().map {this.root.file(it).toPath()}.forEach forEach@{oldPath ->
                val customPath: String? = extension.run.path
                val runPath = if (customPath === null) cache.resolve("run") else Path.of(customPath)

                if (Files.exists(runPath) && !Files.isDirectory(runPath)) {
                    throw FileAlreadyExistsException(runPath.toFile(), reason = "exists and is not a directory")
                }

                if (Files.exists(oldPath)) {
                    if (Files.isSymbolicLink(oldPath)) {
                        val linkTarget: Path = Files.readSymbolicLink(oldPath)

                        if (Files.isSameFile(linkTarget, runPath)) {
                            Files.createDirectories(runPath)

                            return@forEach
                        }

                        if (!Files.exists(linkTarget)) {
                            Files.delete(oldPath)
                        }
                    }

                    if (Files.exists(runPath)) {
                        if (Files.isDirectory(oldPath) && Files.list(oldPath).count() == 0L) {
                            Files.delete(oldPath)
                        } else {
                            makeOld(oldPath)
                        }
                    } else if (Files.isDirectory(oldPath)) {
                        Files.move(oldPath, runPath)
                    }
                }

                Files.createDirectories(runPath)
                Files.createSymbolicLink(oldPath, runPath)
            }
        }
    }

    private fun generateSources() {
        if (extension.genSources !== null && !sourcePath.exists()) {
            log("Sources not found; executing ${extension.genSources}.")
            this.execute(extension.genSources!!)
        }
    }

    companion object {
        @Throws(IOException::class)
        private fun makeOld(path: Path) {
            val moveOld = "$path-old"
            var moveOldPath = Path.of(moveOld + 0)
            var i = 1

            while (Files.exists(moveOldPath)) {
                moveOldPath = Path.of(moveOld + i)
                i++
            }

            Files.move(path, moveOldPath)
        }

        val latestYarnBuilds: MutableMap<String, String> = HashMap()
    }
}
