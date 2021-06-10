package user11681.wheel;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.artifacts.repositories.PasswordCredentials;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.LoggingManager;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
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
import user11681.reflect.Accessor;
import user11681.reflect.Classes;
import user11681.uncheck.Uncheck;
import user11681.wheel.dependency.WheelDependencyFactory;
import user11681.wheel.dependency.configuration.BloatedDependencySet;
import user11681.wheel.dependency.configuration.IntransitiveDependencySet;
import user11681.wheel.extension.WheelExtension;
import user11681.wheel.loader.TransformingClassLoader;
import user11681.wheel.repository.WheelRepositoryFactory;
import user11681.wheel.util.ThrowingAction;

@SuppressWarnings({"ResultOfMethodCallIgnored", "UnstableApiUsage"})
public abstract class WheelPlugin<E extends WheelExtension> implements Plugin<Project> {
    public final String MOD = "mod";

    public static Project currentProject;

    protected static String latestMinecraftVersion;

    private static HttpClient httpClient;

    protected Project project;
    protected Project rootProject;
    protected PluginContainer plugins;
    protected PluginManager pluginManager;
    protected Convention convention;
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
    protected JavaPluginConvention javaConvention;
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

    protected void enqueue(Task task) {
        this.finished(() -> execute(task));
    }

    protected void enqueue(String task) {
        this.finished(() -> execute(this.task(task)));
    }

    private static void execute(Task task) {
        task.getTaskDependencies().getDependencies(task).forEach(WheelPlugin::execute);
        task.getActions().forEach(action -> action.execute(task));
    }

    protected void apply(Project project, String plugin, Class<E> extensionClass) {
        this.transform();

        if (project.getPlugins().hasPlugin(plugin)) {
            throw new IllegalStateException("%s must be either specified before wheel without being applied or not specified at all.".formatted(plugin));
        }

        latestMinecraftVersion = null;

        this.project = currentProject = project;
        this.rootProject = project.getRootProject();
        this.plugins = project.getPlugins();
        this.pluginManager = project.getPluginManager();
        this.convention = project.getConvention();
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
        this.extension = this.extensions.create("wheel", extensionClass);

        project.afterEvaluate((ThrowingAction<Project>) ignored -> this.afterEvaluate());

        Classes.reinterpret(Accessor.getObject(this.repositories, "repositoryFactory"), WheelRepositoryFactory.klass);
        Classes.reinterpret(Accessor.getObject(this.dependencies, "dependencyFactory"), WheelDependencyFactory.klass);

        this.plugins.apply(plugin);
        this.applyPlugins();
        this.addRepositories();
        this.configureConfigurations();
    }

    protected void transform() {}

    protected void configurePublication(MavenPublication publication) {
        publication.setGroupId(String.valueOf(this.project.getGroup()));
        publication.setArtifactId(this.project.getName());
        publication.setVersion(String.valueOf(this.project.getVersion()));
    }

    protected void afterEvaluate() throws Throwable {
        this.project.afterEvaluate((ThrowingAction<Project>) ignored -> this.afterMain());

        this.checkVersions();

        this.<JavaCompile>task("compileJava").setSourceCompatibility(this.compatibilityVersion(this.extension.java.source));
        this.<JavaCompile>task("compileJava").setTargetCompatibility(this.compatibilityVersion(this.extension.java.target));

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
                publishing.getRepositories().maven((MavenArtifactRepository repository) -> {
                    repository.setUrl(this.extension.publish.external.repository);

                    repository.credentials(((PasswordCredentials credentials) -> {
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
        this.javaConvention = this.convention.getPlugin(JavaPluginConvention.class);
        this.sourceSets = this.javaConvention.getSourceSets();
    }

    protected void addRepositories() {
        this.repositories.mavenLocal();
    }

    protected void addDependencies() {
        this.dependencies.add("testImplementation", "org.junit.jupiter:junit-jupiter:+");
    }

    protected void configureConfigurations() {
        this.configurations.all(configuration -> configuration.resolutionStrategy(strategy -> {
            strategy.cacheDynamicVersionsFor(0, "seconds");
            strategy.cacheChangingModulesFor(0, "seconds");
        }));

        Configuration intransitiveInclude = this.configurations.create("intransitiveInclude").setTransitive(false);
        Configuration intransitive = this.configurations.create("intransitive").extendsFrom(intransitiveInclude).setTransitive(false);
        Configuration bloatedInclude = this.configurations.create("bloatedInclude");
        Configuration bloated = this.configurations.create("bloated").extendsFrom(bloatedInclude);
        Configuration modInclude = this.configurations.create("modInclude").extendsFrom(bloatedInclude, intransitiveInclude);
        Configuration apiInclude = this.configurations.create("apiInclude");

        this.configurations.create(MOD).extendsFrom(modInclude, bloated, intransitive);

        Classes.reinterpret(bloated.getDependencies(), BloatedDependencySet.klass);
        Classes.reinterpret(bloatedInclude.getDependencies(), BloatedDependencySet.klass);
        Classes.reinterpret(intransitive.getDependencies(), IntransitiveDependencySet.klass);
        Classes.reinterpret(intransitiveInclude.getDependencies(), IntransitiveDependencySet.klass);

        this.configuration("api").extendsFrom(apiInclude);
        this.configuration("include").extendsFrom(apiInclude, modInclude);
    }

    protected void checkVersions() {
        this.checkMinecraftVersion();
        this.logger.lifecycle("Minecraft version: {}", this.extension.minecraft);
    }

    protected String compatibilityVersion(Object version) {
        return "8";
    }

    protected boolean isRoot() {
        return this.project == this.rootProject;
    }

    protected <T extends Task> TaskCollection<T> tasks(Class<T> type) {
        return this.tasks.withType(type);
    }

    protected <T extends Task> T task(Class<T> type) {
        TaskCollection<T> tasks = this.tasks.withType(type);
        assert tasks.size() == 1;

        return tasks.iterator().next();
    }

    protected <T extends Task> T task(String name) {
        return (T) this.tasks.getByName(name);
    }

    protected SourceSet sourceSet(String name) {
        return this.sourceSets.getByName(name);
    }

    protected Configuration configuration(String name) {
        return this.configurations.getByName(name);
    }

    protected void finished(Runnable action) {
        this.gradle.buildFinished(result -> action.run());
    }

    protected File file(Object path) {
        return this.project.file(path);
    }

    static {
        Classes.reinterpret(WheelPlugin.class.getClassLoader(), TransformingClassLoader.klass);
    }
}
