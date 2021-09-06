package net.auoeke.wheel.extension.publish

import groovy.lang.Closure
import org.gradle.api.Project

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "unused")
class PublishingConfig(private val project: Project) {
    val external = ExternalRepositoryConfig()
    var publication = PublicationConfig()
    var enabled = true
    var local = true

    fun external(action: Closure<*>?) {
        this.project.configure(this.external, action)
    }

    fun publication(action: Closure<*>?) {
        this.project.configure(this.publication, action)
    }

    fun setPublication(enabled: Boolean) {
        this.publication.enabled = enabled
    }
}
