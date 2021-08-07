package net.auoeke.wheel;

import net.auoeke.wheel.util.GroovyUtil;
import com.github.jengelman.gradle.plugins.shadow.ShadowBasePlugin;
import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin;
import java.io.File;
import java.util.stream.Stream;
import net.minecraftforge.gradle.userdev.DependencyManagementExtension;
import net.minecraftforge.gradle.userdev.UserDevExtension;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.maven.MavenPublication;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.gradle.plugins.MixinExtension;
import org.spongepowered.asm.gradle.plugins.MixinGradlePlugin;
import user11681.reflect.Accessor;
import user11681.reflect.Classes;
import net.auoeke.wheel.extension.WheelForgeExtension;

@SuppressWarnings("unused")
public class WheelForgePlugin extends AbstractWheelForgePlugin<WheelForgePlugin, WheelForgeExtension> {
    protected static final String GEN_INTELLIJ_RUNS = "genIntellijRuns";
    protected static final String MINECRAFT = "minecraft";

    private DependencyManagementExtension dependencyExtension;
    private UserDevExtension userdevExtension;

    @Override
    public void apply(@NotNull Project project) {
        super.apply(project, "net.minecraftforge.gradle", new WheelForgeExtension(project));

        this.defaultTask(GEN_INTELLIJ_RUNS);
    }

    @Override
    public void checkMinecraftVersion() {
        super.checkMinecraftVersion();

        this.userdevExtension.mappings("official", this.extension.minecraft);
    }

    @Override
    protected void transform() {
        loader.transform("net.minecraftforge.gradle.common.util.MojangLicenseHelper", type -> withMethod("displayWarning", type, method -> {
            method.instructions.clear();
            method.instructions.add(new InsnNode(Opcodes.RETURN));
        }));
        loader.transform("net.minecraftforge.gradle.common.util.Utils", type -> {
            String lambdaName = "lambda$createRunConfigTasks$13";
            Type projectType = Type.getType(Project.class);

            withMethod(lambdaName, type, method -> {
                Type[] argumentTypes = Type.getArgumentTypes(method.desc);
                argumentTypes[argumentTypes.length - 1] = projectType;
                method.desc = Type.getMethodDescriptor(Type.getType(void.class), argumentTypes);
            });
            withMethod("createRunConfigTasks", type, method -> {
                AbstractInsnNode instruction = method.instructions.getFirst();

                while (instruction != null) {
                    if (instruction instanceof MethodInsnNode invocation) {
                        switch (invocation.name) {
                            case "projectsEvaluated" -> {
                                invocation.owner = Type.getInternalName(Project.class);
                                invocation.name = "afterEvaluate";
                            }
                            case "getGradle" -> {
                                instruction = instruction.getNext();
                                method.instructions.remove(instruction.getPrevious());
                                instruction = instruction.getPrevious();
                            }
                        }
                    } else if (instruction instanceof InvokeDynamicInsnNode lambda) {
                        Handle handle = (Handle) lambda.bsmArgs[1];

                        if (handle.getName().equals(lambdaName)) {
                            Type[] argumentTypes = Type.getArgumentTypes(handle.getDesc());
                            argumentTypes[argumentTypes.length - 1] = projectType;

                            lambda.bsmArgs[1] = new Handle(handle.getTag(), handle.getOwner(), handle.getName(), Type.getMethodDescriptor(Type.getType(void.class), argumentTypes), false);
                            lambda.bsmArgs[2] = Type.getMethodType(Type.getType(void.class), projectType);
                        }
                    }

                    instruction = instruction.getNext();
                }
            });
        });
    }

    @Override
    public void configurePublication(MavenPublication publication) {
        super.configurePublication(publication);

        publication.artifact(this.artifacts().add("default", new File("%s/libs/%s-%s-obf.jar".formatted(this.project.getBuildDir(), this.name(), this.project.getVersion())), artifact -> {
            artifact.setType("jar");
            artifact.builtBy("reobfJar");
        }));
    }

    @Override
    protected void applyPlugins() {
        super.applyPlugins();

        this.apply(MixinGradlePlugin.class);
        this.apply(ShadowPlugin.class);

        this.dependencyExtension = this.extension(DependencyManagementExtension.class);
        this.userdevExtension = this.extension(UserDevExtension.class);
        this.extension(MixinExtension.class).add(this.sourceSet("main"), this.name() + ".refmap.json");

        this.generateRunConfigurations();

        Accessor.putObject(GroovyUtil.site(Classes.load("org.spongepowered.asm.gradle.plugins.MixinExtension$_init_closure1"), 10), "name", "implementation");
    }

    @Override
    public void addDependencies() {
        super.addDependencies();

        this.dependency(MINECRAFT, "net.minecraftforge:forge:%s-%s".formatted(this.extension.minecraft, this.extension.forge));
        this.dependency(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, "org.spongepowered:mixin:latest.integration");

        if (!Boolean.getBoolean("idea.sync.active")) {
            this.dependency(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME, "org.spongepowered:mixin:latest.integration:processor");
        }

        this.configuration(MOD).getDependencies().forEach(this.dependencyExtension::deobf);
        this.configuration(ShadowBasePlugin.getCONFIGURATION_NAME()).getDependencies().forEach(dependency -> {

        });
    }

    @Override
    public void configureConfigurations() {
        this.configuration(ShadowBasePlugin.getCONFIGURATION_NAME()).extendsFrom(this.configurations().create("include"));

        super.configureConfigurations();
    }

    private void generateRunConfigurations() {
        Stream.of("client", "server").forEach(side -> this.userdevExtension.getRuns().create(side, configuration -> {
            configuration.workingDirectory(this.file("run"));

            configuration.property("forge.logging.console.level", "debug");
            configuration.property("mixin.env.remapRefMap", "true");
            configuration.property("mixin.env.refMapRemappingFile", "%s/createSrgToMcp/output.srg".formatted(this.project.getBuildDir()));
            configuration.arg("-mixin.config=%s.mixins.json".formatted(this.name()));

            configuration.getMods().register(this.name(), mod -> mod.source(this.sourceSet("main")));
        }));
    }
}
