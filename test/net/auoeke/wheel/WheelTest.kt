package net.auoeke.wheel

import org.gradle.testkit.runner.GradleRunner
import java.io.File

abstract class WheelTest {
    protected fun run(vararg arguments: String): GradleRunner = GradleRunner.create().withProjectDir(File("test/project")).withPluginClasspath().withArguments(arguments.toMutableList() + "--stacktrace").forwardOutput()
    protected fun debug(vararg arguments: String): GradleRunner = GradleRunner.create().withProjectDir(File("test/project")).withDebug(true).withPluginClasspath().withArguments(arguments.toMutableList() + "--stacktrace").forwardOutput()
}
