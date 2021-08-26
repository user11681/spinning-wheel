package net.auoeke.wheel.extension.publish

import org.gradle.api.Action

class PublishingConfig {
    val external = ExternalRepositoryConfig()
    val publication = PublicationConfig()
    var enabled = true
    var local = true

    fun external(action: Action<ExternalRepositoryConfig?>) {
        action.execute(external)
    }

    fun publication(action: Action<PublicationConfig?>) {
        action.execute(publication)
    }

    fun setPublication(enabled: Boolean) {
        publication.enabled = enabled
    }
}
