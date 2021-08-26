package net.auoeke.wheel.dependency

import net.auoeke.reflect.Classes
import net.auoeke.wheel.WheelPlugin
import net.auoeke.wheel.extension.WheelExtension
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.artifacts.DefaultDependencyFactory
import net.auoeke.wheel.extension.dependency.Dependency as WheelDependency

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class WheelDependencyFactory : DefaultDependencyFactory(null, null, null, null, null, null) {
    override fun createDependency(dependencyNotation: Any): Dependency {
        return if (dependencyNotation is String) super.createDependency(resolve(dependencyNotation)) else super.createDependency(dependencyNotation)
    }

    companion object {
        val klass = Classes.klass(WheelDependencyFactory::class.java)
        private fun changeVersion(artifact: String, version: String): String {
            val segments = artifact.split(":") as MutableList
            segments[2] = version

            return segments.joinToString(":")
        }

        private fun addRepository(repository: String?) {
            if (WheelPlugin.currentProject != null && repository != null) {
                val repositories = WheelPlugin.currentProject!!.repositories

                for (artifactRepository in repositories) {
                    if (artifactRepository is MavenArtifactRepository && repository == artifactRepository.url.toString()) {
                        return
                    }
                }

                repositories.maven {it.setUrl(repository)}
            }
        }

        private fun addRepository(entry: WheelDependency?): Boolean {
            if (entry != null) {
                addRepository(entry.resolveRepository())

                return true
            }

            return false
        }

        private fun resolve(dependency: String): Any {
            val components = dependency.split(":") as MutableList

            if (components.size == 2) {
                val entry = WheelExtension.dependency(components[0])

                if (addRepository(entry)) {
                    return changeVersion(entry.artifact, components[1])
                }
            }

            val entry = WheelExtension.dependency(dependency)

            return if (addRepository(entry)) {
                entry.artifact
            } else if (WheelPlugin.currentProject != null && WheelPlugin.currentProject!!.findProject(dependency) != null) {
                WheelPlugin.currentProject!!.dependencies.project(mapOf("path" to dependency))
            } else dependency
        }
    }
}
