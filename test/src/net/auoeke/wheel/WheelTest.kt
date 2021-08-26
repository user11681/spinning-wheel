package net.auoeke.wheel

import groovy.transform.CompileStatic
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.platform.commons.annotation.Testable
import java.io.File
import java.util.*

@CompileStatic
@Testable
class WheelTest {
    @Test
    fun testBuild() {
        GradleRunner.create().withProjectDir(File("test/project")).withPluginClasspath(File("build/pluginMetadata").listFiles()!!.flatMap map@{metadata ->
            val properties = Properties()
            properties.load(metadata.inputStream())

            return@map properties.getProperty("implementation-classpath").split(":").map {File(it)}
        }).withArguments("--stacktrace").build()
    }
}
