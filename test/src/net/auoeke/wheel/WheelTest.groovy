package net.auoeke.wheel

import groovy.transform.CompileStatic
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.platform.commons.annotation.Testable

@CompileStatic
@Testable
class WheelTest {
    @Test
    void testBuild() {
        GradleRunner.create().withProjectDir("test/project" as File).withPluginClasspath(("build/pluginMetadata" as File).listFiles().collectMany {
            var metadata = new Properties()
            metadata.load(it.newInputStream())

            return metadata.getProperty("implementation-classpath").split(":").collect {it as File}
        }).withArguments("--stacktrace").build()
    }
}
