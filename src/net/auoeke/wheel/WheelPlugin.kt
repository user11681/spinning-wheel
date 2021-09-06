package net.auoeke.wheel

import net.auoeke.reflect.Accessor
import net.auoeke.reflect.Classes
import net.auoeke.wheel.dependency.WheelDependencyFactory
import net.auoeke.wheel.extension.WheelExtension
import net.auoeke.wheel.loader.TransformingClassLoader
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.util.function.Consumer

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
abstract class WheelPlugin<P : WheelPlugin<P, E>, E : WheelExtension> : WheelPluginBase<E> {
    override lateinit var project: Project
    override lateinit var extension: E

    protected fun apply(project: Project, plugin: String, extension: E) {
        currentProject = project
        this.project = project
        this.extension = extension
        this.extensions.add("wheel", extension)

        if (transformed.add(this.javaClass)) {
            this.transform()
        }

        if (this.plugins.hasPlugin(plugin)) {
            throw IllegalStateException("$plugin must be either specified before wheel without being applied or not specified at all.")
        }

        if (this.isRoot) {
            latestMinecraftVersion = null
        }

        this.afterEvaluation {this.afterEvaluation()}

        Classes.reinterpret<Any>(Accessor.getObject(this.dependencies, "dependencyFactory"), WheelDependencyFactory.klass)

        this.beforeMain()
        this.plugins.apply(plugin)
        this.applyPlugins()
        this.addRepositories()
        this.configureConfigurations()
    }

    protected open fun transform() {}
    protected open fun beforeMain() {}

    protected open fun applyPlugins() {
        this.apply<JavaLibraryPlugin>()
        this.apply<MavenPublishPlugin>()

        this.java.withSourcesJar()
    }

    protected open fun addRepositories() {
        this.repositories.mavenLocal()

        this.repositories.all {repository ->
            if (repository is MavenArtifactRepository) {
                val url = WheelExtension.repository(repository.name)

                if (url !== null) {
                    repository.setUrl(url)
                }
            }
        }
    }

    protected inline fun <reified T : Plugin<Project>> WheelPluginBase<*>.apply() = this.plugins.apply(T::class.java)

    companion object {
        val loader: TransformingClassLoader = Classes.reinterpret(WheelPlugin::class.java.classLoader, TransformingClassLoader.klass)
        private val transformed: MutableSet<Class<out Any>> = HashSet()

        var latestMinecraftVersion: String? = null

        // This does not work. Todo: fix
        var currentProject: Project? = null

        @JvmStatic
        protected fun withMethod(name: String, type: ClassNode, action: Consumer<MethodNode>) {
            type.methods.stream().filter {method -> method.name.equals(name)}.forEach(action)
        }
    }
}
