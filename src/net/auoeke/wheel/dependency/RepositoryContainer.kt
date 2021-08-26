package net.auoeke.wheel.dependency

import net.auoeke.wheel.extension.dependency.Dependency
import net.auoeke.wheel.extension.dependency.Repository
import net.auoeke.wheel.util.ObservableMap
import net.auoeke.wheel.util.Util.sanitize
import org.gradle.api.Action

@Suppress("NAME_SHADOWING")
class RepositoryContainer {
    val repositories = ObservableMap<String, Repository>()

    fun configure(action: Action<RepositoryContainer>): RepositoryContainer {
        action.execute(this)

        return this
    }

    fun repository(key: String): String? {
        return repositories[sanitize(key)]?.url
    }

    fun repository(key: String, url: String): Repository {
        return repositories.computeIfAbsent(sanitize(key)) {Repository(it, url)}!!
    }

    fun putRepository(key: String, url: String) {
        val key = sanitize(key)
        repositories[key] = Repository(key, url)
    }

    fun entry(dependency: String): Dependency {
        val sanitizedDependency = sanitize(dependency)

        return repositories.values.stream()
            .flatMap {it!!.dependencies.stream()}
            .filter {entry -> sanitize(entry.key) == sanitizedDependency || sanitize(entry.artifact.split(':', limit = 3).toMutableList()[1]) == sanitizedDependency}
            .findFirst()
            .orElse(null)
    }
}
