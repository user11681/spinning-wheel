package net.auoeke.wheel;

import net.auoeke.wheel.dependency.WheelDependencyFactory;
import net.auoeke.wheel.extension.WheelExtension;
import net.auoeke.wheel.loader.TransformingClassLoader;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import user11681.reflect.Accessor;
import user11681.reflect.Classes;

@SuppressWarnings({"SameParameterValue", "unused"})
public abstract class WheelPlugin<P extends WheelPlugin<P, E>, E extends WheelExtension> implements WheelPluginBase<E> {
    public static final TransformingClassLoader loader = Classes.reinterpret(WheelPlugin.class.getClassLoader(), TransformingClassLoader.klass);
    public static Project currentProject;

    protected static String latestMinecraftVersion;

    private static final Set<Class<?>> transformed = new HashSet<>();

    protected Project project;
    protected E extension;

    protected static void withMethod(String name, ClassNode type, Consumer<MethodNode> action) {
        type.methods.stream().filter(method -> method.name.equals(name)).forEach(action);
    }

    @Override
    public Project project() {
        return this.project;
    }

    @Override
    public E extension() {
        return this.extension;
    }

    protected final void apply(Project project, String plugin, E extension) {
        this.project = currentProject = project;
        this.extension = extension;
        this.extensions().add("wheel", extension);

        if (transformed.add(this.getClass())) {
            this.transform();
        }

        if (this.plugins().hasPlugin(plugin)) {
            throw new IllegalStateException("%s must be either specified before wheel without being applied or not specified at all.".formatted(plugin));
        }

        if (this.isRoot()) {
            latestMinecraftVersion = null;
        }

        this.afterEvaluation(this::afterEvaluation);

        Classes.reinterpret(Accessor.getObject(this.dependencies(), "dependencyFactory"), WheelDependencyFactory.klass);

        this.beforeMain();
        this.plugins().apply(plugin);
        this.applyPlugins();
        this.addRepositories();
        this.configureConfigurations();
    }

    protected void transform() {}

    protected void beforeMain() {}

    protected void applyPlugins() {
        this.apply(JavaLibraryPlugin.class);
        this.apply(MavenPublishPlugin.class);

        this.java().withSourcesJar();
    }

    protected void addRepositories() {
        this.repositories().mavenLocal();

        this.repositories().all(repository -> {
            if (repository instanceof MavenArtifactRepository mavenRepository) {
                Optional.ofNullable(WheelExtension.repository(repository.getName())).ifPresent(mavenRepository::setUrl);
            }
        });
    }
}
