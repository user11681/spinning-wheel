package net.auoeke.wheel.extension.publish

class ExternalRepositoryConfig {
    var repository: String? = System.getProperty("wheel.repository")
    var username: String? = System.getProperty("wheel.username")
    var password: String? = System.getProperty("wheel.password")
}
