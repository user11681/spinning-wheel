package net.auoeke.wheel.dependency

import net.auoeke.wheel.extension.dependency.Dependency
import net.auoeke.wheel.extension.dependency.Repository
import net.auoeke.wheel.util.ObservableMap
import net.auoeke.wheel.util.sanitize

@Suppress("NAME_SHADOWING")
class RepositoryContainer {
    private val repositories: ObservableMap<String, Repository> = ObservableMap()

    fun configure(action: RepositoryContainer.() -> Unit): RepositoryContainer = this.also {action(this)}

    fun repository(key: String): String? = this.repositories[sanitize(key)]?.url

    fun repository(key: String, url: String, configurator: Repository.() -> Unit = {}) = configurator(this.repositories.computeIfAbsent(sanitize(key)) {Repository(it, url)})

    fun putRepository(key: String, url: String) {
        val key = sanitize(key)
        this.repositories[key] = Repository(key, url)
    }

    fun entry(dependency: String): Dependency? {
        val sanitized = sanitize(dependency)

        return this.repositories.values
            .flatMap(Repository::dependencies)
            .find {entry -> sanitize(entry.key) == sanitized || sanitize(entry.artifact.split(':', limit = 3)[1]) == sanitized}
    }
}
