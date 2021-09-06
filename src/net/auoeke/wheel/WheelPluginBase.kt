package net.auoeke.wheel

import net.auoeke.extensions.string
import net.auoeke.extensions.type
import net.auoeke.wheel.extension.WheelExtension
import net.auoeke.wheel.util.tryCatch
import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.artifacts.dsl.ArtifactHandler
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.initialization.dsl.ScriptHandler
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import org.gradle.api.logging.LoggingManager
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.PluginManager
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.internal.Actions
import org.gradle.jvm.tasks.Jar
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*
import kotlin.reflect.KClass

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "UNCHECKED_CAST", "UnstableApiUsage")
interface WheelPluginBase<E : WheelExtension> : Plugin<Project> {
    val project: Project
    val extension: E
    val metadataFile: String
    val defaultJavaVersion: String get() = "8"
    val name: String get() = this.project.name
    val plugins: PluginManager get() = this.project.pluginManager
    val java: JavaPluginExtension get() = this.extension(JavaPluginExtension::class)
    val sourceSets: SourceSetContainer get() = this.java.sourceSets
    val tasks: TaskContainer get() = this.project.tasks
    val extensions: ExtensionContainer get() = this.project.extensions
    val repositories: RepositoryHandler get() = this.project.repositories
    val dependencies: DependencyHandler get() = this.project.dependencies
    val configurations: ConfigurationContainer get() = this.project.configurations
    val artifacts: ArtifactHandler get() = this.project.artifacts
    val logger: Logger get() = this.project.logger
    val gradle: Gradle get() = this.project.gradle
    val buildScript: ScriptHandler get() = this.project.buildscript
    val logging: LoggingManager get() = this.project.logging
    val extra: ExtraPropertiesExtension get() = this.extensions.extraProperties
    val root: Project get() = this.project.rootProject
    val isRoot: Boolean get() = this.project == this.root
    val defaultTasks: MutableList<String> get() = this.project.defaultTasks

    fun checkMinecraftVersion()
    fun afterMain() {}

    fun checkVersions() {
        this.checkMinecraftVersion()
        this.log("Minecraft version: ${this.extension.minecraft}")
    }

    fun addDependencies() {
        this.dependency("testImplementation", "org.junit.jupiter:junit-jupiter:latest.integration")
    }

    private fun javaVersion(version: Any?): String {
        return (if (version == "latest") JavaVersion.current() else version ?: this.defaultJavaVersion).string
    }

    fun configurePublication(publication: MavenPublication) {
        publication.groupId = this.project.group.string
        publication.artifactId = this.name
        publication.version = this.project.version.string
    }

    fun configureConfigurations() {
        this.configurations.all {configuration ->
            configuration.resolutionStrategy {resolution ->
                resolution.cacheDynamicVersionsFor(0, "seconds")
                resolution.cacheChangingModulesFor(0, "seconds")

                resolution.eachDependency {dependency ->
                    val target = dependency.target

                    when (val group = target.group) {
                        "curseforge", "cf" -> dependency.useTarget(target.string.replaceFirst(group, "curse.maven"))
                        "jitpack" -> {
                            val components = target.string.split(':') as MutableList<String>
                            val repository = target.name.split('/', limit = 2)
                            components[0] = "com.github.${repository[0]}"
                            components[1] = repository[1]

                            dependency.useTarget(components.joinToString(":"))
                        }
                    }
                }
            }

            configuration.dependencies.all {dependency ->
                if (dependency is ExternalModuleDependency && dependency.group != null) {
                    when (dependency.group) {
                        "curse.maven", "curseforge", "cf" -> this.repository("curse-maven", "Curse Maven")
                        "jitpack" -> this.repository("jitpack", "JitPack")
                    }
                }
            }
        }

        val intransitiveInclude = this.createConfiguration("intransitiveInclude").setTransitive(false)
        val bloatedInclude = this.createConfiguration("bloatedInclude")
        val modInclude = this.createConfiguration("modInclude").extendsFrom(bloatedInclude, intransitiveInclude)
        val apiInclude = this.createConfiguration("apiInclude")
        val bloated = this.createConfiguration("bloated") {
            it.allDependencies.all {dependency ->
                if (dependency is ModuleDependency) {
                    dependency.exclude(mapOf("module" to "fabric-api"))
                }
            }
        }.extendsFrom(bloatedInclude)
        val intransitive = this.createConfiguration("intransitive") {
            it.allDependencies.all {dependency ->
                if (dependency is ModuleDependency) {
                    dependency.isTransitive = false
                }
            }
        }.extendsFrom(intransitiveInclude).setTransitive(false)

        this.createConfiguration(MOD).extendsFrom(modInclude, bloated, intransitive)

        this.configuration("api").extendsFrom(apiInclude)
        this.configuration("include").extendsFrom(apiInclude, modInclude)
    }

    fun afterEvaluation() {
        this.afterEvaluation {this.afterMain()}

        this.checkVersions()

        this.task<JavaCompile>("compileJava").sourceCompatibility = this.javaVersion(this.extension.java.source)
        this.task<JavaCompile>("compileJava").targetCompatibility = this.javaVersion(this.extension.java.target)

        this.addDependencies()

        if (this.extension.clean) {
            this.task<Task>("build").dependsOn("clean")
            this.task<Task>("clean").finalizedBy("build")
        }

        this.tasks(JavaCompile::class).all {task -> task.options.encoding = "UTF-8"}
        this.tasks(Jar::class).all {task -> task.from("LICENSE")}

        this.tasks(ProcessResources::class).all {task ->
            task.filesMatching(this.metadataFile) {file ->
                file.filter {it.replace("\\$(\\{version}|version)", this.project.version.string)}
            }
        }

        if (this.extension.publish.enabled) {
            val publishing = this.extension(PublishingExtension::class)

            if (this.extension.publish.publication.enabled) {
                publishing.publications.create(this.extension.publish.publication.name, type<MavenPublication>(), this::configurePublication)
            }

            if (this.extension.publish.local) {
                publishing.repositories.mavenLocal()
            }

            if (this.extension.publish.external.repository != null) {
                publishing.repositories.maven {repository ->
                    repository.setUrl(this.extension.publish.external.repository)

                    repository.credentials {credentials ->
                        credentials.username = this.extension.publish.external.username
                        credentials.password = this.extension.publish.external.password
                    }
                }
            }
        }
    }

    fun afterEvaluation(action: Action<in Project>) = this.project.afterEvaluate(action)

    fun <T : Any> extension(type: KClass<T>): T = this.extensions.getByType(type.java)

    fun <T : Task> task(type: KClass<T>): T {
        val tasks = this.tasks.withType(type.java)
        assert(tasks.size == 1)

        return tasks.iterator().next()
    }

    fun sourceSet(name: String): SourceSet = this.sourceSets.getByName(name)

    fun configuration(name: String): Configuration = this.configurations.getByName(name)

    fun createConfiguration(name: String): Configuration = this.createConfiguration(name, Actions.doNothing())

    fun createConfiguration(name: String, initializer: Action<Configuration>): Configuration = this.configurations.create(name, initializer)

    fun file(path: Any): File = this.project.file(path)

    fun <T : Task> tasks(type: KClass<T>): TaskCollection<T> = this.tasks.withType(type.java)

    @SuppressWarnings("unchecked")
    fun <T : Task> task(name: String): T = this.tasks.getByName(name) as T

    fun defaultTask(vararg tasks: String) = Collections.addAll(this.defaultTasks, *tasks)

    fun log(message: String, vararg arguments: Any) = this.logger.lifecycle(message, arguments)

    fun dependency(configuration: String, dependencyNotation: Any): Dependency? = this.dependencies.add(configuration, dependencyNotation)

    fun repository(url: String, name: String? = null): MavenArtifactRepository {
        val resolvedURL = {URL(url)}.tryCatch {URL(WheelExtension.repository(url))}.toURI()

        return this.repositories
            .find {repository -> repository is MavenArtifactRepository && resolvedURL.equals(repository.url)} as MavenArtifactRepository?
            ?: this.repositories.maven {repository ->
                repository.name = name
                repository.url = resolvedURL
            }
    }

    fun execute(task: Task) {
        task.taskDependencies.getDependencies(task).forEach(this::execute)
        task.actions.forEach {action -> action.execute(task)}
    }

    fun execute(task: String) = this.execute(this.task(task))

    fun finished(action: Runnable) = this.gradle.buildFinished {action.run()}

    fun enqueue(task: String) = this.gradle.taskGraph.whenReady {this.execute(task)}

    fun enqueue(task: Task) = this.gradle.taskGraph.whenReady {this.execute(task)}

    fun get(uri: String): String = http.send(HttpRequest.newBuilder(URI(uri)).build(), HttpResponse.BodyHandlers.ofString()).body()

    companion object {
        const val MOD: String = "mod"

        val http: HttpClient by lazy(HttpClient::newHttpClient)
    }
}
