package net.auoeke.wheel.dependency

import net.auoeke.extensions.string
import net.auoeke.extensions.then
import net.auoeke.extensions.type
import net.auoeke.reflect.Classes
import net.auoeke.wheel.extension.WheelExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.artifacts.DefaultDependencyFactory
import net.auoeke.wheel.extension.dependency.Dependency as WheelDependency

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class WheelDependencyFactory : DefaultDependencyFactory(null, null, null, null, null, null) {
    override fun createDependency(dependencyNotation: Any): Dependency = super.createDependency(when (dependencyNotation) {
        is String -> this.resolve(dependencyNotation)
        else -> dependencyNotation
    })

    private fun withVersion(artifact: String, version: String): String = (artifact.split(":") as MutableList).also {
        when {
            it.size > 2 -> it[2] = version
            else -> it += version
        }
    }.joinToString(":")

    private fun addRepository(repository: String?) {
        if (project !== null && repository !== null) {
            val repositories = project!!.repositories

            for (artifactRepository in repositories) {
                if (artifactRepository is MavenArtifactRepository && repository == artifactRepository.url.string) {
                    return
                }
            }

            repositories.maven {it.setUrl(repository)}
        }
    }

    private fun addRepository(entry: WheelDependency?): Boolean = (entry !== null).then {
        this.addRepository(entry!!.resolveRepository())
    }

    private fun resolve(dependency: String): Any {
        val components = dependency.split(":") as MutableList

        if (components.size == 2) {
            val entry = WheelExtension.dependency(components[0])

            return when (this.addRepository(entry)) {
                true -> this.withVersion(entry!!.artifact, components[1])
                false -> this.withVersion(dependency, "latest.release")
            }
        }

        val entry = WheelExtension.dependency(dependency)

        return when {
            this.addRepository(entry) -> entry!!.artifact
            project?.findProject(dependency) !== null -> project!!.dependencies.project(mapOf("path" to dependency))
            else -> dependency
        }
    }

    companion object {
        val klass = Classes.klass(type<WheelDependencyFactory>())
        var project: Project? = null
    }
}
