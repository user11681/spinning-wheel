package user11681.wheel;

import com.github.jengelman.gradle.plugins.shadow.ShadowBasePlugin;
import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin;
import java.util.Objects;
import java.util.function.Consumer;
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
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.gradle.plugins.MixinExtension;
import org.spongepowered.asm.gradle.plugins.MixinGradlePlugin;
import org.w3c.dom.Node;
import user11681.uncheck.Uncheck;
import user11681.wheel.extension.WheelForgeExtension;
import user11681.wheel.loader.TransformingClassLoader;

@SuppressWarnings("unused")
public class WheelForgePlugin extends WheelPlugin<WheelForgeExtension> {
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

    private static void withMethod(String name, ClassNode type, Consumer<MethodNode> action) {
        type.methods.stream().filter(method -> method.name.equals(name)).forEach(action);
    }

    @Override
    public void apply(Project project) {
        super.apply(project, "net.minecraftforge.gradle", WheelForgeExtension.class);
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
        }

        this.userdevExtension.mappings("official", this.extension.minecraft);
    }

    @Override
    protected void transform() {
        TransformingClassLoader.addTransformer("net.minecraftforge.gradle.common.util.MojangLicenseHelper", type -> {
            withMethod("displayWarning", type, method -> {
                method.instructions.clear();
                method.instructions.add(new InsnNode(Opcodes.RETURN));
            });
        });
        TransformingClassLoader.addTransformer("net.minecraftforge.gradle.common.util.Utils", type -> {
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
                    } else {
                        if (instruction instanceof InvokeDynamicInsnNode lambda) {
                            Handle handle = (Handle) lambda.bsmArgs[1];

                            if (handle.getName().equals(lambdaName)) {
                                Type[] argumentTypes = Type.getArgumentTypes(handle.getDesc());
                                argumentTypes[argumentTypes.length - 1] = projectType;

                                lambda.bsmArgs[1] = new Handle(handle.getTag(), handle.getOwner(), handle.getName(), Type.getMethodDescriptor(Type.getType(void.class), argumentTypes), false);
                                lambda.bsmArgs[2] = Type.getMethodType(Type.getType(void.class), projectType);
                            }
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

        publication.artifact(this.artifacts.add("default", "%s/libs/%s-%s-obf.jar".formatted(this.project.getBuildDir(), this.project.getName(), this.project.getVersion()), artifact -> {
            artifact.setType("jar");
            artifact.builtBy("reobfJar");
        }));
    }

    @Override
    protected void afterMain() {
        this.generateRunConfigurations();

        this.enqueue(GEN_INTELLIJ_RUNS);
    }

    @Override
    protected void applyPlugins() {
        super.applyPlugins();

        this.plugins.apply(MixinGradlePlugin.class);
        this.plugins.apply(ShadowPlugin.class);

        this.dependencyExtension = this.extensions.getByType(DependencyManagementExtension.class);
        this.userdevExtension = this.extensions.getByType(UserDevExtension.class);
        this.extensions.getByType(MixinExtension.class).add(this.sourceSet("main"), this.project.getName() + ".refmap.json");
    }

    @Override
    protected void addDependencies() {
        super.addDependencies();

        this.dependencies.add("minecraft", "net.minecraftforge:forge:%s-%s".formatted(this.extension.minecraft, this.extension.forge));
        this.dependencies.add("implementation", "org.spongepowered:mixin:+");
        this.dependencies.add("annotationProcessor", "org.spongepowered:mixin:+:processor");

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

    private void generateRunConfigurations() {
        Stream.of("client", "server").forEach(side -> this.userdevExtension.getRuns().register(side, configuration -> {
            configuration.workingDirectory(this.file("run"));
            configuration.arg("-mixin.config=%s.mixins.json".formatted(this.project.getName()));
            configuration.property("forge.logging.console.level", "debug");

            configuration.getMods().register(this.project.getName(), mod -> mod.source(this.sourceSet("main")));
        }));
    }
}
