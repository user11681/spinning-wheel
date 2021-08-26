package net.auoeke.wheel

import com.github.jengelman.gradle.plugins.shadow.ShadowBasePlugin
import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import net.auoeke.reflect.Accessor
import net.auoeke.reflect.Classes
import net.auoeke.wheel.extension.WheelForgeExtension
import net.auoeke.wheel.util.GroovyUtil
import net.minecraftforge.gradle.userdev.DependencyManagementExtension
import net.minecraftforge.gradle.userdev.UserDevExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.maven.MavenPublication
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.spongepowered.asm.gradle.plugins.MixinExtension
import org.spongepowered.asm.gradle.plugins.MixinGradlePlugin
import java.io.File

class WheelForgePlugin : AbstractWheelForgePlugin<WheelForgePlugin, WheelForgeExtension>() {
    private lateinit var dependencyExtension: DependencyManagementExtension
    private lateinit var userdevExtension: UserDevExtension

    override fun apply(project: Project) {
        super.apply(project, "net.minecraftforge.gradle", WheelForgeExtension(project))

        this.defaultTask(GEN_INTELLIJ_RUNS)
    }

    override fun checkMinecraftVersion() {
        super.checkMinecraftVersion()

        this.userdevExtension.mappings("official", this.extension.minecraft!!)
    }

    override fun transform() {
        loader.transform("net.minecraftforge.gradle.common.util.MojangLicenseHelper") {
            withMethod("displayWarning", it) {method ->
                method.instructions.clear()
                method.instructions.add(InsnNode(Opcodes.RETURN))
            }
        }
        loader.transform("net.minecraftforge.gradle.common.util.Utils") {type ->
            val lambdaName = "lambda\$createRunConfigTasks\$13"
            val projectType = Type.getType(Project::class.java)
            val void = Void.TYPE

            withMethod(lambdaName, type) {method ->
                val argumentTypes = Type.getArgumentTypes(method.desc)
                argumentTypes[argumentTypes.size - 1] = projectType
                method.desc = Type.getMethodDescriptor(Type.getType(void), *argumentTypes)
            }
            withMethod("createRunConfigTasks", type) {method ->
                var instruction = method.instructions.first

                while (instruction != null) {
                    if (instruction is MethodInsnNode) {
                        when (instruction.name) {
                            "projectsEvaluated" -> {
                                instruction.owner = Type.getInternalName(Project::class.java)
                                instruction.name = "afterEvaluate"
                            }
                            "getGradle" -> {
                                instruction = instruction.next
                                method.instructions.remove(instruction.previous)
                                instruction = instruction.previous
                            }
                        }
                    } else if (instruction is InvokeDynamicInsnNode) {
                        val handle = instruction.bsmArgs[1] as Handle

                        if (handle.name == lambdaName) {
                            val argumentTypes = Type.getArgumentTypes(handle.desc)
                            argumentTypes[argumentTypes.size - 1] = projectType

                            instruction.bsmArgs[1] = Handle(
                                handle.tag,
                                handle.owner,
                                handle.name,
                                Type.getMethodDescriptor(Type.getType(void), *argumentTypes),
                                false
                            )
                            instruction.bsmArgs[2] = Type.getMethodType(Type.getType(void), projectType)
                        }
                    }

                    instruction = instruction.next
                }
            }
        }
    }

    override fun configurePublication(publication: MavenPublication) {
        super.configurePublication(publication)

        publication.artifact(this.artifacts.add("default", File("${this.project.buildDir}/libs/${this.name}-${this.project.version}-obf.jar")) {artifact ->
            artifact.type = "jar"
            artifact.builtBy("reobfJar")
        })
    }

    override fun applyPlugins() {
        super.applyPlugins()

        this.apply(MixinGradlePlugin::class)
        this.apply(ShadowPlugin::class)

        this.dependencyExtension = this.extension(DependencyManagementExtension::class)
        this.userdevExtension = this.extension(UserDevExtension::class)
        this.extension(MixinExtension::class).add(this.sourceSet("main"), "${this.name}.refmap.json")

        this.generateRunConfigurations()

        Accessor.putObject(GroovyUtil.site(Classes.load<Any>("org.spongepowered.asm.gradle.plugins.MixinExtension\$_init_closure1"), 10), "name", "implementation")
    }

    override fun addDependencies() {
        super.addDependencies()

        this.dependency(MINECRAFT, "net.minecraftforge:forge:${this.extension.minecraft}-${this.extension.forge}")
        this.dependency(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, "org.spongepowered:mixin:latest.integration")

        if (!System.getProperty("idea.sync.active").toBoolean()) {
            this.dependency(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME, "org.spongepowered:mixin:latest.integration:processor")
        }

        this.configuration(WheelPluginBase.MOD).dependencies.forEach(this.dependencyExtension::deobf)
//        this.configuration(ShadowBasePlugin.getCONFIGURATION_NAME()).dependencies.forEach {}
    }

    override fun configureConfigurations() {
        this.configuration(ShadowBasePlugin.getCONFIGURATION_NAME()).extendsFrom(this.createConfiguration("include"))

        super.configureConfigurations()
    }

    private fun generateRunConfigurations() {
        listOf("client", "server").forEach {side ->
            this.userdevExtension.runs.create(side) {configuration ->
                configuration.workingDirectory(this.file("run"))

                configuration.property("forge.logging.console.level", "debug")
                configuration.property("mixin.env.remapRefMap", "true")
                configuration.property("mixin.env.refMapRemappingFile", "${this.project.buildDir}/createSrgToMcp/output.srg")
                configuration.arg("-mixin.config=${this.name}.mixins.json")

                configuration.mods.register(this.name) {it.source(this.sourceSet("main"))}
            }
        }
    }

    companion object {
        private const val GEN_INTELLIJ_RUNS: String = "genIntellijRuns"
        private const val MINECRAFT: String = "minecraft"
    }
}
