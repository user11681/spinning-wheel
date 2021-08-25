package net.auoeke.wheel;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.auoeke.wheel.extension.WheelExtension;
import net.auoeke.wheel.util.Lazy;
import org.gradle.api.Action;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ModuleVersionSelector;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.LoggingManager;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.PluginManager;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.internal.Actions;
import org.gradle.jvm.tasks.Jar;
import org.gradle.language.jvm.tasks.ProcessResources;
import user11681.uncheck.ThrowingRunnable;
import user11681.uncheck.Uncheck;

@SuppressWarnings("UnusedReturnValue")
public interface WheelPluginBase<E extends WheelExtension> extends Plugin<Project> {
    String MOD = "mod";

    Lazy<HttpClient> http = Lazy.of(() -> HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build());

    Project project();

    E extension();

    String metadataFile();

    void checkMinecraftVersion();

    default void afterMain() {}

    default void checkVersions() {
        this.checkMinecraftVersion();
        this.log("Minecraft version: {}", this.extension().minecraft);
    }

    default void addDependencies() {
        this.dependency("testImplementation", "org.junit.jupiter:junit-jupiter:latest.integration");
    }

    default String defaultJavaVersion() {
        return "8";
    }

    private String javaVersion(Object version) {
        if (version == null) {
            return this.defaultJavaVersion();
        }

        return (version.equals("latest") ? JavaVersion.current() : version).toString();
    }

    default void configurePublication(MavenPublication publication) {
        publication.setGroupId(String.valueOf(this.project().getGroup()));
        publication.setArtifactId(this.name());
        publication.setVersion(String.valueOf(this.project().getVersion()));
    }

    default void configureConfigurations() {
        this.configurations().all(configuration -> {
            configuration.resolutionStrategy(resolution -> {
                resolution.cacheDynamicVersionsFor(0, "seconds");
                resolution.cacheChangingModulesFor(0, "seconds");

                resolution.eachDependency(dependency -> {
                    ModuleVersionSelector target = dependency.getTarget();

                    switch (target.getGroup()) {
                        case "curseforge", "cf" -> dependency.useTarget(target.toString().replaceFirst("curseforge|cf", "curse.maven"));
                        case "jitpack" -> {
                            String[] components = target.toString().split(":");
                            String[] repository = target.getName().split("/", 2);
                            components[0] = "com.github." + repository[0];
                            components[1] = repository[1];

                            dependency.useTarget(String.join(":", components));
                        }
                    }
                });
            });

            configuration.getDependencies().all(dependency -> {
                if (dependency instanceof ExternalModuleDependency && dependency.getGroup() != null) {
                    switch (dependency.getGroup()) {
                        case "curse.maven", "curseforge", "cf" -> this.repository("Curse Maven", "curse-maven");
                        case "jitpack" -> this.repository("JitPack", "jitpack");
                    }
                }
            });
        });

        Configuration intransitiveInclude = this.createConfiguration("intransitiveInclude").setTransitive(false);
        Configuration bloatedInclude = this.createConfiguration("bloatedInclude");
        Configuration modInclude = this.createConfiguration("modInclude").extendsFrom(bloatedInclude, intransitiveInclude);
        Configuration apiInclude = this.createConfiguration("apiInclude");
        Configuration bloated = this.createConfiguration("bloated", configuration -> configuration.getAllDependencies().all(dependency -> {
            if (dependency instanceof ModuleDependency moduleDependency) {
                moduleDependency.exclude(Map.of("module", "fabric-api"));
            }
        })).extendsFrom(bloatedInclude);
        Configuration intransitive = this.createConfiguration("intransitive", configuration -> configuration.getAllDependencies().all(dependency -> {
            if (dependency instanceof ModuleDependency moduleDependency) {
                moduleDependency.setTransitive(false);
            }
        })).extendsFrom(intransitiveInclude).setTransitive(false);

        this.createConfiguration(MOD).extendsFrom(modInclude, bloated, intransitive);

        this.configuration("api").extendsFrom(apiInclude);
        this.configuration("include").extendsFrom(apiInclude, modInclude);
    }

    default void afterEvaluation() {
        this.afterEvaluation(this::afterMain);

        this.checkVersions();

        this.<JavaCompile>task("compileJava").setSourceCompatibility(this.javaVersion(this.extension().java.source));
        this.<JavaCompile>task("compileJava").setTargetCompatibility(this.javaVersion(this.extension().java.target));

        this.addDependencies();

        if (this.extension().clean) {
            this.task("build").dependsOn("clean");
            this.task("clean").finalizedBy("build");
        }

        this.tasks(JavaCompile.class).all(task -> task.getOptions().setEncoding("UTF-8"));
        this.tasks(Jar.class).all(task -> task.from("LICENSE"));

        this.tasks(ProcessResources.class).all(task -> task.filesMatching(
            this.metadataFile(),
            file -> file.filter(contents -> contents.replaceAll("\\$(\\{version}|version)", String.valueOf(this.project().getVersion())))
        ));

        if (this.extension().publish.enabled) {
            PublishingExtension publishing = this.extension(PublishingExtension.class);

            if (this.extension().publish.publication.enabled) {
                publishing.getPublications().create(this.extension().publish.publication.name, MavenPublication.class, this::configurePublication);
            }

            if (this.extension().publish.local) {
                publishing.getRepositories().mavenLocal();
            }

            if (this.extension().publish.external.repository != null) {
                publishing.getRepositories().maven(repository -> {
                    repository.setUrl(this.extension().publish.external.repository);

                    repository.credentials(credentials -> {
                        credentials.setUsername(this.extension().publish.external.username);
                        credentials.setPassword(this.extension().publish.external.password);
                    });
                });
            }
        }
    }

    default void afterEvaluation(ThrowingRunnable action) {
        this.project().afterEvaluate(ignored -> action.run());
    }

    default PluginManager plugins() {
        return this.project().getPluginManager();
    }

    default void apply(Class<? extends Plugin<Project>> plugin) {
        this.plugins().apply(plugin);
    }

    default JavaPluginExtension java() {
        return this.extension(JavaPluginExtension.class);
    }

    default SourceSetContainer sourceSets() {
        return this.java().getSourceSets();
    }

    default TaskContainer tasks() {
        return this.project().getTasks();
    }

    default ExtensionContainer extensions() {
        return this.project().getExtensions();
    }

    default RepositoryHandler repositories() {
        return this.project().getRepositories();
    }

    default DependencyHandler dependencies() {
        return this.project().getDependencies();
    }

    default ConfigurationContainer configurations() {
        return this.project().getConfigurations();
    }

    default ArtifactHandler artifacts() {
        return this.project().getArtifacts();
    }

    default Logger logger() {
        return this.project().getLogger();
    }

    default Gradle gradle() {
        return this.project().getGradle();
    }

    default ScriptHandler buildScript() {
        return this.project().getBuildscript();
    }

    default LoggingManager logging() {
        return this.project().getLogging();
    }

    default ExtraPropertiesExtension extra() {
        return this.extensions().getExtraProperties();
    }

    default Project root() {
        return this.project().getRootProject();
    }

    default boolean isRoot() {
        return this.project() == this.root();
    }

    default <T> T extension(Class<T> type) {
        return this.extensions().getByType(type);
    }

    default <T extends Task> T task(Class<T> type) {
        TaskCollection<T> tasks = this.tasks().withType(type);
        assert tasks.size() == 1;

        return tasks.iterator().next();
    }

    default SourceSet sourceSet(String name) {
        return this.sourceSets().getByName(name);
    }

    default Configuration configuration(String name) {
        return this.configurations().getByName(name);
    }

    default Configuration createConfiguration(String name) {
        return this.createConfiguration(name, Actions.doNothing());
    }

    default Configuration createConfiguration(String name, Action<Configuration> initializer) {
        return this.configurations().create(name, initializer);
    }

    default File file(Object path) {
        return this.project().file(path);
    }

    default String name() {
        return this.project().getName();
    }

    default List<String> defaultTasks() {
        return this.project().getDefaultTasks();
    }

    default <T extends Task> TaskCollection<T> tasks(Class<T> type) {
        return this.tasks().withType(type);
    }

    @SuppressWarnings("unchecked")
    default <T extends Task> T task(String name) {
        return (T) this.tasks().getByName(name);
    }

    default void defaultTask(String... tasks) {
        Collections.addAll(this.defaultTasks(), tasks);
    }

    default void log(String message, Object... arguments) {
        this.logger().lifecycle(message, arguments);
    }

    default Dependency dependency(String configuration, Object dependencyNotation) {
        return this.dependencies().add(configuration, dependencyNotation);
    }

    default MavenArtifactRepository repository(String name, String url) {
        URI resolvedURL = Uncheck.handle(() -> Uncheck.handle(() -> new URL(url), () -> new URL(WheelExtension.repository(url))).toURI());

        return this.repositories().stream()
            .filter(repository -> repository instanceof MavenArtifactRepository mavenRepository && resolvedURL.equals(mavenRepository.getUrl()))
            .map(MavenArtifactRepository.class::cast)
            .findAny()
            .orElseGet(() -> this.repositories().maven(repository -> {
                repository.setName(name);
                repository.setUrl(resolvedURL);
            }));
    }

    default MavenArtifactRepository repository(String url) {
        return this.repository(null, url);
    }

    default void execute(String task) {
        this.execute(this.task(task));
    }

    default void execute(Task task) {
        task.getTaskDependencies().getDependencies(task).forEach(this::execute);
        task.getActions().forEach(action -> action.execute(task));
    }

    default void finished(Runnable action) {
        this.gradle().buildFinished(result -> action.run());
    }

    default void enqueue(String task) {
        this.gradle().getTaskGraph().whenReady(graph -> this.execute(task));
    }

    default void enqueue(Task task) {
        this.gradle().getTaskGraph().whenReady(graph -> this.execute(task));
    }

    default String get(String uri) {
        return Uncheck.handle(() -> http.get().send(HttpRequest.newBuilder(new URI(uri)).GET().build(), HttpResponse.BodyHandlers.ofString()).body());
    }
}
