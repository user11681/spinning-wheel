package user11681.wheel;

import com.github.jengelman.gradle.plugins.shadow.ShadowBasePlugin;
import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin;
import java.io.File;
import java.util.Objects;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import net.minecraftforge.gradle.userdev.DependencyManagementExtension;
import net.minecraftforge.gradle.userdev.UserDevExtension;
import org.gradle.api.Project;
import org.gradle.api.publish.maven.MavenPublication;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.gradle.plugins.MixinExtension;
import org.spongepowered.asm.gradle.plugins.MixinGradlePlugin;
import org.w3c.dom.Node;
import user11681.reflect.Accessor;
import user11681.reflect.Classes;
import user11681.uncheck.Uncheck;
import user11681.wheel.extension.WheelForgeExtension;
import user11681.wheel.util.GroovyUtil;

@SuppressWarnings("unused")
public class WheelForgePlugin extends WheelPlugin<WheelForgePlugin, WheelForgeExtension> {
    private static final String FORGE_URL = "https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml";
    private static final String GEN_INTELLIJ_RUNS = "genIntellijRuns";

    private DependencyManagementExtension dependencyExtension;
    private UserDevExtension userdevExtension;

    private static Node versioning() {
        return Uncheck.handle(() -> DocumentBuilderFactory
            .newDefaultInstance()
            .newDocumentBuilder()
            .parse(FORGE_URL)
            .getFirstChild()
            .getLastChild()
        );
    }

    private static Stream<String> nodeStream(Node first) {
        return Stream.iterate(first, Objects::nonNull, Node::getNextSibling).map(Node::getTextContent);
    }

    @Override
    public void apply(Project project) {
        super.apply(project, "net.minecraftforge.gradle", new WheelForgeExtension());

        this.defaultTask("genIntellijRuns");
    }

    @Override
    protected String metadataFile() {
        return "mods.toml";
    }

    @Override
    protected void checkMinecraftVersion() {
        Node versioning = null;

        if (this.extension.minecraft == null) {
            versioning = versioning();

            if (latestMinecraftVersion == null) {
                if (this.extension.forge == null) {
                    String[] versions = versioning.getChildNodes().item(1).getTextContent().split("-", 2);

                    this.extension.minecraft = latestMinecraftVersion = versions[0];
                    this.extension.forge = versions[1];
                } else {
                    this.extension.minecraft = nodeStream(versioning.getLastChild().getFirstChild())
                        .filter(version -> version.endsWith(this.extension.forge))
                        .findFirst()
                        .map(version -> version.substring(0, version.indexOf('-')))
                        .orElseThrow(() -> new IllegalArgumentException("Forge version \"%s\" was not found at %s.".formatted(this.extension.forge, FORGE_URL)));
                }
            } else {
                this.extension.minecraft = latestMinecraftVersion;
            }
        }

        if (this.extension.forge == null) {
            this.extension.forge = nodeStream((versioning == null ? versioning() : versioning).getLastChild().getFirstChild())
                .filter(version -> version.startsWith(this.extension.minecraft))
                .findFirst()
                .map(version -> version.substring(version.indexOf('-') + 1))
                .orElseThrow(() -> new IllegalArgumentException("Minecraft version \"%s\" was not found at %s.".formatted(this.extension.minecraft, FORGE_URL)));
        } else if (this.extension.minecraft.equals("1.16.5")) {
            this.extension.forge = "36.1.31";
        }

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
    protected void configurePublication(MavenPublication publication) {
        super.configurePublication(publication);

        publication.artifact(this.artifacts.add("default", new File("%s/libs/%s-%s-obf.jar".formatted(this.project.getBuildDir(), this.name(), this.project.getVersion())), artifact -> {
            artifact.setType("jar");
            artifact.builtBy("reobfJar");
        }));
    }

    @Override
    protected void applyPlugins() {
        super.applyPlugins();

        this.plugins.apply(MixinGradlePlugin.class);
        this.plugins.apply(ShadowPlugin.class);

        this.dependencyExtension = this.extensions.getByType(DependencyManagementExtension.class);
        this.userdevExtension = this.extensions.getByType(UserDevExtension.class);
        this.extensions.getByType(MixinExtension.class).add(this.sourceSet("main"), this.name() + ".refmap.json");

        this.generateRunConfigurations();

        Accessor.putObject(GroovyUtil.site(Classes.load("org.spongepowered.asm.gradle.plugins.MixinExtension$_init_closure1"), 10), "name", "implementation");
    }

    @Override
    protected void addDependencies() {
        super.addDependencies();

        this.dependencies.add("minecraft", "net.minecraftforge:forge:%s-%s".formatted(this.extension.minecraft, this.extension.forge));
        this.dependencies.add("implementation", "org.spongepowered:mixin:latest.integration");

        if (!Boolean.getBoolean("idea.sync.active")) {
            this.dependencies.add("annotationProcessor", "org.spongepowered:mixin:latest.integration:processor");
        }

        this.configuration(MOD).getDependencies().forEach(this.dependencyExtension::deobf);
        this.configuration(ShadowBasePlugin.getCONFIGURATION_NAME()).getDependencies().forEach(dependency -> {

        });
    }

    @Override
    protected void configureConfigurations() {
        this.configuration(ShadowBasePlugin.getCONFIGURATION_NAME()).extendsFrom(this.configurations.create("include"));

        super.configureConfigurations();
    }

    @Override
    protected void checkVersions() {
        super.checkVersions();

        this.logger.lifecycle("Forge version: {}", this.extension.forge);
    }

    @Override
    protected String defaultJavaVersion() {
        return Integer.parseInt(this.extension.minecraft.split("\\.", 3)[1]) >= 17 ? "16" : "8";
    }

    private void generateRunConfigurations() {
        Stream.of("client", "server").forEach(side -> this.userdevExtension.getRuns().create(side, configuration -> {
            configuration.workingDirectory(this.file("run"));

            // configuration.environment("target", side);
            configuration.property("forge.logging.console.level", "debug");
            configuration.property("mixin.env.remapRefMap", "true");
            configuration.property("mixin.env.refMapRemappingFile", "%s/createSrgToMcp/output.srg".formatted(this.project.getBuildDir()));
            // configuration.main("net.minecraftforge.userdev.LaunchTesting");
            configuration.arg("-mixin.config=%s.mixins.json".formatted(this.name()));

            configuration.getMods().register(this.name(), mod -> mod.source(this.sourceSet("main")));
        }));
    }
}
