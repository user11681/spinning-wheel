package user11681.wheel;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.LoggingManager;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.plugins.PluginManager;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.tasks.Jar;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import user11681.reflect.Accessor;
import user11681.reflect.Classes;
import user11681.uncheck.Uncheck;
import user11681.wheel.dependency.WheelDependencyFactory;
import user11681.wheel.extension.WheelExtension;
import user11681.wheel.loader.TransformingClassLoader;
import user11681.wheel.util.ThrowingAction;

@SuppressWarnings({"SameParameterValue", "unused"})
public abstract class WheelPlugin<P extends WheelPlugin<P, E>, E extends WheelExtension> implements Plugin<Project> {
    public static final String MOD = "mod";
    public static final TransformingClassLoader loader = Classes.reinterpret(WheelPlugin.class.getClassLoader(), TransformingClassLoader.klass);
    public static Project currentProject;

    protected static String latestMinecraftVersion;

    private static final Set<Class<?>> transformed = new HashSet<>();
    private static HttpClient httpClient;

    protected Project project;
    protected Project rootProject;
    protected PluginContainer plugins;
    protected PluginManager pluginManager;
    protected TaskContainer tasks;
    protected ExtensionContainer extensions;
    protected RepositoryHandler repositories;
    protected DependencyHandler dependencies;
    protected ConfigurationContainer configurations;
    protected ArtifactHandler artifacts;
    protected Logger logger;
    protected Gradle gradle;
    protected ScriptHandler buildScript;
    protected LoggingManager logging;
    protected E extension;
    protected JavaPluginExtension java;
    protected SourceSetContainer sourceSets;

    protected abstract String metadataFile();

    protected abstract void checkMinecraftVersion();

    protected static HttpClient http() {
        return httpClient == null ? httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build() : httpClient;
    }

    protected static String get(String uri) {
        return Uncheck.handle(() -> http().send(HttpRequest.newBuilder(new URI(uri)).GET().build(), HttpResponse.BodyHandlers.ofString()).body());
    }

    protected static void makeOld(Path path) throws IOException {
        String moveOld = path.toString() + "-old";
        Path moveOldPath = Path.of(moveOld + 0);

        for (int i = 1; Files.exists(moveOldPath); i++) {
            moveOldPath = Path.of(moveOld + i);
        }

        Files.move(path, moveOldPath);
    }

    protected static void withMethod(String name, ClassNode type, Consumer<MethodNode> action) {
        type.methods.stream().filter(method -> method.name.equals(name)).forEach(action);
    }

    public <T extends Task> T task(Class<T> type) {
        TaskCollection<T> tasks = this.tasks.withType(type);
        assert tasks.size() == 1;

        return tasks.iterator().next();
    }

    public <T extends Task> T task(String name) {
        return (T) this.tasks.getByName(name);
    }

    public SourceSet sourceSet(String name) {
        return this.sourceSets.getByName(name);
    }

    public Configuration configuration(String name) {
        return this.configurations.getByName(name);
    }

    public File file(Object path) {
        return this.project.file(path);
    }

    public String name() {
        return this.project.getName();
    }

    public List<String> defaultTasks() {
        return this.project.getDefaultTasks();
    }

    public void defaultTask(String... tasks) {
        Collections.addAll(this.defaultTasks(), tasks);
    }

    public boolean isRoot() {
        return this.project == this.rootProject;
    }

    public <T extends Task> TaskCollection<T> tasks(Class<T> type) {
        return this.tasks.withType(type);
    }

    public MavenArtifactRepository repository(String name, String url) {
        URI resolvedURL = Uncheck.handle(() -> Uncheck.handle(() -> new URL(url), () -> new URL(WheelExtension.repository(url))).toURI());

        return this.repositories.stream()
            .filter(repository -> repository instanceof MavenArtifactRepository mavenRepository && resolvedURL.equals(mavenRepository.getUrl()))
            .map(MavenArtifactRepository.class::cast)
            .findAny()
            .orElseGet(() -> this.repositories.maven(repository -> {
                repository.setName(name);
                repository.setUrl(resolvedURL);
            }));
    }

    public MavenArtifactRepository repository(String url) {
        return this.repository(null, url);
    }

    protected void execute(String task) {
        this.execute(this.task(task));
    }

    protected void execute(Task task) {
        task.getTaskDependencies().getDependencies(task).forEach(this::execute);
        task.getActions().forEach(action -> action.execute(task));
    }

    protected void finished(Runnable action) {
        this.gradle.buildFinished(result -> action.run());
    }

    protected void enqueue(String task) {
        this.gradle.getTaskGraph().whenReady(graph -> this.execute(task));
    }

    protected void enqueue(Task task) {
        this.gradle.getTaskGraph().whenReady(graph -> this.execute(task));
    }

    protected final void apply(Project project, String plugin, E extension) {
        if (transformed.add(this.getClass())) {
            this.transform();
        }

        if (project.getPlugins().hasPlugin(plugin)) {
            throw new IllegalStateException("%s must be either specified before wheel without being applied or not specified at all.".formatted(plugin));
        }

        if (this.isRoot()) {
            latestMinecraftVersion = null;
        }

        this.project = currentProject = project;
        this.rootProject = project.getRootProject();
        this.plugins = project.getPlugins();
        this.pluginManager = project.getPluginManager();
        this.tasks = project.getTasks();
        this.extensions = project.getExtensions();
        this.repositories = project.getRepositories();
        this.dependencies = project.getDependencies();
        this.configurations = project.getConfigurations();
        this.artifacts = project.getArtifacts();
        this.logger = project.getLogger();
        this.gradle = project.getGradle();
        this.buildScript = project.getBuildscript();
        this.logging = project.getLogging();
        this.extension = extension;
        this.extensions.add("wheel", extension);

        project.afterEvaluate((ThrowingAction<Project>) ignored -> this.afterEvaluate());

        Classes.reinterpret(Accessor.getObject(this.dependencies, "dependencyFactory"), WheelDependencyFactory.klass);

        this.plugins.apply(plugin);
        this.applyPlugins();
        this.addRepositories();
        this.configureConfigurations();
    }

    protected void transform() {}

    protected void configurePublication(MavenPublication publication) {
        publication.setGroupId(String.valueOf(this.project.getGroup()));
        publication.setArtifactId(this.name());
        publication.setVersion(String.valueOf(this.project.getVersion()));
    }

    protected void afterEvaluate() throws Throwable {
        this.project.afterEvaluate((ThrowingAction<Project>) ignored -> this.afterMain());

        this.checkVersions();

        this.<JavaCompile>task("compileJava").setSourceCompatibility(this.javaVersion(this.extension.java.source));
        this.<JavaCompile>task("compileJava").setTargetCompatibility(this.javaVersion(this.extension.java.target));

        this.addDependencies();

        if (this.extension.clean) {
            this.task("build").dependsOn("clean");
            this.task("clean").finalizedBy("build");
        }

        this.tasks(JavaCompile.class).forEach(task -> task.getOptions().setEncoding("UTF-8"));
        this.tasks(Jar.class).forEach(task -> task.from("LICENSE"));

        this.tasks(ProcessResources.class).forEach(task -> task.filesMatching(
            this.metadataFile(),
            file -> file.filter(contents -> contents.replaceAll("\\$(\\{version}|version)", String.valueOf(this.project.getVersion())))
        ));

        if (this.extension.publish.enabled) {
            PublishingExtension publishing = this.extensions.getByType(PublishingExtension.class);

            if (this.extension.publish.publication.enabled) {
                publishing.getPublications().create(this.extension.publish.publication.name, MavenPublication.class, this::configurePublication);
            }

            if (this.extension.publish.local) {
                publishing.getRepositories().mavenLocal();
            }

            if (this.extension.publish.external.repository != null) {
                publishing.getRepositories().maven(repository -> {
                    repository.setUrl(this.extension.publish.external.repository);

                    repository.credentials((credentials -> {
                        credentials.setUsername(this.extension.publish.external.username);
                        credentials.setPassword(this.extension.publish.external.password);
                    }));
                });
            }
        }
    }

    protected void afterMain() throws Throwable {}

    protected void applyPlugins() {
        this.plugins.apply(JavaLibraryPlugin.class);
        this.plugins.apply(MavenPublishPlugin.class);

        this.java = this.extensions.getByType(JavaPluginExtension.class);
        this.java.withSourcesJar();
        this.sourceSets = this.java.getSourceSets();
    }

    protected void addRepositories() {
        this.repositories.mavenLocal();

        this.repositories.all(repository -> {
            if (repository instanceof MavenArtifactRepository mavenRepository) {
                Optional.ofNullable(WheelExtension.repository(repository.getName())).ifPresent(mavenRepository::setUrl);
            }
        });
    }

    protected void addDependencies() {
        this.dependencies.add("testImplementation", "org.junit.jupiter:junit-jupiter:+");
    }

    protected void configureConfigurations() {
        this.configurations.all(configuration -> {
            configuration.resolutionStrategy(strategy -> {
                strategy.cacheDynamicVersionsFor(0, "seconds");
                strategy.cacheChangingModulesFor(0, "seconds");
            });

            configuration.getDependencies().all(dependency -> {
                if (dependency instanceof ExternalModuleDependency moduleDependency && dependency.getGroup() != null) {
                    switch (dependency.getGroup()) {
                        case "curse.maven", "curseforge", "cf" -> this.repository("Curse Maven", "curse-maven");
                        case "jitpack" -> this.repository("JitPack", "jitpack");
                    }
                }
            });
        });

        Configuration intransitiveInclude = this.configurations.create("intransitiveInclude").setTransitive(false);
        Configuration bloatedInclude = this.configurations.create("bloatedInclude");
        Configuration modInclude = this.configurations.create("modInclude").extendsFrom(bloatedInclude, intransitiveInclude);
        Configuration apiInclude = this.configurations.create("apiInclude");
        Configuration bloated = this.configurations.create("bloated", configuration -> configuration.getAllDependencies().all(dependency -> {
            if (dependency instanceof ModuleDependency moduleDependency) {
                moduleDependency.exclude(Map.of("module", "fabric-api"));
            }
        })).extendsFrom(bloatedInclude);
        Configuration intransitive = this.configurations.create("intransitive", configuration -> configuration.getAllDependencies().all(dependency -> {
            if (dependency instanceof ModuleDependency moduleDependency) {
                moduleDependency.setTransitive(false);
            }
        })).extendsFrom(intransitiveInclude).setTransitive(false);

        this.configurations.create(MOD).extendsFrom(modInclude, bloated, intransitive);

        this.configuration("api").extendsFrom(apiInclude);
        this.configuration("include").extendsFrom(apiInclude, modInclude);
    }

    protected void checkVersions() {
        this.checkMinecraftVersion();
        this.logger.lifecycle("Minecraft version: {}", this.extension.minecraft);
    }

    protected String defaultJavaVersion() {
        return "8";
    }

    private String javaVersion(Object version) {
        if (version == null) {
            return this.defaultJavaVersion();
        }

        return (version.equals("latest") ? JavaVersion.current() : version).toString();
    }
}
