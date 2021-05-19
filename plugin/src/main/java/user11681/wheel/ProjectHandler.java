package user11681.wheel;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.LoomGradlePlugin;
import net.fabricmc.loom.configuration.ide.RunConfigSettings;
import net.fabricmc.loom.task.GenerateSourcesTask;
import net.fabricmc.loom.task.RemapJarTask;
import net.fabricmc.loom.task.RunGameTask;
import net.fabricmc.loom.task.UnpickJarTask;
import org.gradle.api.NamedDomainObjectContainer;
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
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.tasks.Jar;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.intellij.lang.annotations.Language;
import user11681.reflect.Accessor;
import user11681.reflect.Classes;
import user11681.reflect.Invoker;
import user11681.uncheck.ThrowingConsumer;
import user11681.uncheck.Uncheck;
import user11681.wheel.dependency.WheelDependencyFactory;
import user11681.wheel.dependency.configuration.BloatedDependencySet;
import user11681.wheel.dependency.configuration.IntransitiveDependencySet;
import user11681.wheel.extension.Channel;
import user11681.wheel.extension.WheelExtension;
import user11681.wheel.repository.WheelRepositoryFactory;
import user11681.wheel.util.ThrowingAction;

@SuppressWarnings({"ResultOfMethodCallIgnored", "UnstableApiUsage"})
public class ProjectHandler {
    public static Project currentProject = null;

    private static final Map<String, String> latestYarnBuilds = new HashMap<>();

    private static String latestMinecraftVersion = null;
    private static HttpClient httpClient = null;

    public final Project project;
    public final Project rootProject;
    public final PluginContainer plugins;
    public final PluginManager pluginManager;
    public final Convention convention;
    public final TaskContainer tasks;
    public final ExtensionContainer extensions;
    public final RepositoryHandler repositories;
    public final DependencyHandler dependencies;
    public final ConfigurationContainer configurations;
    public final ArtifactHandler artifacts;
    public final Logger logger;
    public final Gradle gradle;
    public final ScriptHandler buildScript;
    public final LoggingManager logging;
    public final WheelExtension extension;

    public LoomGradleExtension loom;
    public NamedDomainObjectContainer<RunConfigSettings> runConfigs;

    public ProjectHandler(Project project) {
        this.project = project;
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
        this.extension = this.extensions.create("wheel", WheelExtension.class, this);
    }

    private static String meta(String endpoint, @Language("RegExp") String pattern) {
        if (httpClient == null) {
            httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
        }

        Matcher matcher = Uncheck.handle(() -> Pattern.compile(pattern).matcher(httpClient.send(
            HttpRequest.newBuilder().GET().uri(new URI("https://meta.fabricmc.net/v2/versions/" + endpoint)).build(),
            HttpResponse.BodyHandlers.ofString()
        ).body()));

        matcher.find();

        return matcher.group();
    }

    private static void makeOld(Path path) throws IOException {
        String moveOld = path.toString() + "-old";
        Path moveOldPath = Path.of(moveOld + 0);

        for (int i = 1; Files.exists(moveOldPath); i++) {
            moveOldPath = Path.of(moveOld + i);
        }

        Files.move(path, moveOldPath);
    }

    private void checkMinecraftVersion() {
        if (this.extension.minecraftVersion == null) {
            if (latestMinecraftVersion == null) {
                latestMinecraftVersion = meta(
                    "game",
                    this.extension.channel == Channel.RELEASE
                        ? "(?<=\"version\": \").*?(?=\",\\s*\"stable\": true)"
                        : "(?<=\"version\": \").*?(?=\")"
                );
            }

            this.extension.minecraftVersion = latestMinecraftVersion;
        }
    }

    private void checkYarnBuild() {
        if (this.extension.yarnBuild == null) {
            if (latestYarnBuilds.get(this.extension.minecraftVersion) == null) {
                latestYarnBuilds.put(this.extension.minecraftVersion, meta(
                    "yarn/" + this.extension.minecraftVersion,
                    "(?<=\"build\": )\\d+"
                ));
            }

            this.extension.yarnBuild = latestYarnBuilds.get(this.extension.minecraftVersion);
        }
    }

    private <T extends Task> T task(String name) {
        return (T) this.tasks.getByName(name);
    }

    private <T extends Task> TaskCollection<T> task(Class<T> type) {
        return this.tasks.withType(type);
    }

    public void handle() {
        this.project.afterEvaluate(ignored -> this.afterEvaluate());

        currentProject = this.project;

        Classes.reinterpret(Accessor.getObject(this.repositories, "repositoryFactory"), WheelRepositoryFactory.klass);
        Classes.reinterpret(Accessor.getObject(this.dependencies, "dependencyFactory"), WheelDependencyFactory.klass);

        this.plugins.apply(JavaLibraryPlugin.class);
        this.plugins.apply(MavenPublishPlugin.class);
        this.plugins.apply(LoomGradlePlugin.class);

        this.extensions.getByType(JavaPluginExtension.class).withSourcesJar();

        // this.configurations.create("dev");

        this.loom = this.extensions.getByType(LoomGradleExtension.class);
        // this.loom.shareCaches = false; // shareCaches = true prevents dev JAR remapping for some reason
        this.runConfigs = this.loom.getRunConfigs();

        this.extension.javaVersion.setSource(8);
        this.extension.javaVersion.setTarget(8);

        this.repositories.mavenLocal();

        this.configureConfigurations();
    }

    private void afterEvaluate() {
        SourceSet test = this.convention.getPlugin(JavaPluginConvention.class).getSourceSets().getByName("test");

        this.runConfigs.create("testClient", settings -> {
            settings.client();
            settings.source(test);

            this.runTask(settings).classpath(test.getRuntimeClasspath());
        });

        this.runConfigs.create("testServer", settings -> {
            settings.server();
            settings.source(test);

            this.runTask(settings).classpath(test.getRuntimeClasspath());
        });

        this.checkMinecraftVersion();
        this.checkYarnBuild();

        this.logger.lifecycle("Minecraft version: {}", this.extension.minecraftVersion);
        this.logger.lifecycle("Yarn build: {}", this.extension.yarnBuild);

        this.dependencies.add("minecraft", "com.mojang:minecraft:" + this.extension.minecraftVersion);
        this.dependencies.add("mappings", "net.fabricmc:yarn:%s+build.%s:v2".formatted(this.extension.minecraftVersion, this.extension.yarnBuild));
        this.dependencies.add("mod", "net.fabricmc:fabric-loader:latest.release");
        this.dependencies.add("testImplementation", "org.junit.jupiter:junit-jupiter:latest.release");

        if (this.extension.nospam) {
            this.dependencies.add("mod", "narrator-off");
            this.dependencies.add("mod", "noauth");
        }

        if (this.extension.clean) {
            this.task("build").dependsOn("clean");
            this.task("clean").finalizedBy("build");
        }

        this.task(JavaCompile.class).forEach(task -> task.getOptions().setEncoding("UTF-8"));

        this.task(Jar.class).forEach(task -> {
            // task.getArchiveClassifier().set("dev");

            task.from("LICENSE");
        });

        this.<ProcessResources>task("processResources").filesMatching(
            "fabric.mod.json",
            file -> file.filter(contents -> contents.replaceAll("\\$(\\{version}|version)", String.valueOf(this.project.getVersion())))
        );

        RemapJarTask remapJar = this.task("remapJar");

        //        File devJar = project.file("%s/libs/%s-%s-dev.jar".formatted(this.project.getBuildDir(), this.project.getName(), this.project.getVersion()));
        //
        //        this.artifacts.add("dev", devJar, (ConfigurablePublishArtifact artifact) -> artifact.builtBy(this.task("jar")).setType("jar"));
        //
        //        if (devJar.exists()) {
        //            remapJar.getInput().set(devJar);
        //            remapJar.getArchiveFileName().set("%s-%s.jar".formatted(this.project.getName(), this.project.getVersion()));
        //        }

        if (this.extension.publish.enabled) {
            PublishingExtension publishing = this.extensions.getByType(PublishingExtension.class);

            if (this.extension.publish.publication.enabled) {
                publishing.getPublications().create(this.extension.publish.publication.name, MavenPublication.class, (MavenPublication publication) -> {
                    publication.setGroupId(String.valueOf(this.project.getGroup()));
                    publication.setArtifactId(this.project.getName());
                    publication.setVersion(String.valueOf(this.project.getVersion()));

                    publication.artifact(remapJar).builtBy(remapJar);
                    publication.artifact(this.task("sourcesJar")).builtBy(this.task("remapSourcesJar"));
                });
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

        this.project.afterEvaluate((ThrowingAction<Project>) project -> this.afterLoom());
    }

    private RunGameTask runTask(RunConfigSettings configuration) {
        String name = configuration.getName();

        return this.task("run" + name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1));
    }

    private void afterLoom() throws Throwable {
        this.generateSources();
        this.setRunDirectory();
    }

    private void generateSources() throws Throwable {
        if (this.extension.genSources != null) {
            GenerateSourcesTask genSources = (GenerateSourcesTask) this.tasks.findByName(this.extension.genSources);

            if (genSources != null) {
                if (!((File) Invoker.bind(genSources, "getMappedJarFileWithSuffix", File.class, String.class).invoke("-sources.jar")).exists()) {
                    this.logger.lifecycle("sources not found; running genSources");

                    if (this.loom.getMappingsProvider().hasUnpickDefinitions()) {
                        this.<UnpickJarTask>task("unpickJar").exec();
                    }

                    genSources.doTask();
                }
            }
        }
    }

    private void setRunDirectory() {
        if (this.project == this.rootProject && this.extension.run.enabled) {
            this.runConfigs.stream().map(RunConfigSettings::getRunDir).distinct().map(this.rootProject::file).map(File::toPath).forEach((ThrowingConsumer<Path>) (Path oldPath) -> {
                String customPath = this.extension.run.path;
                Path runPath = customPath == null ? this.loom.getUserCache().toPath().resolve("run") : Path.of(customPath);

                if (Files.exists(runPath) && !Files.isDirectory(runPath)) {
                    throw new FileAlreadyExistsException("%s exists and is not a directory".formatted(runPath));
                }

                if (Files.exists(oldPath)) {
                    if (Files.isSymbolicLink(oldPath)) {
                        Path linkTarget = Files.readSymbolicLink(oldPath);

                        if (Files.isSameFile(linkTarget, runPath)) {
                            Files.createDirectories(runPath);

                            return;
                        }

                        if (!Files.exists(linkTarget)) {
                            Files.delete(oldPath);
                        }
                    }

                    if (Files.exists(runPath)) {
                        makeOld(oldPath);
                    } else if (Files.isDirectory(oldPath)) {
                        Files.move(oldPath, runPath);
                    }
                }

                Files.createDirectories(runPath);
                Files.createSymbolicLink(oldPath, runPath);
            });
        }
    }

    private void configureConfigurations() {
        Configuration intransitiveInclude = this.configurations.create("intransitiveInclude").setTransitive(false);
        Configuration intransitive = this.configurations.create("intransitive").extendsFrom(intransitiveInclude).setTransitive(false);
        Configuration bloatedInclude = this.configurations.create("bloatedInclude");
        Configuration bloated = this.configurations.create("bloated").extendsFrom(bloatedInclude);
        Configuration modInclude = this.configurations.create("modInclude").extendsFrom(bloatedInclude, intransitiveInclude);
        Configuration mod = this.configurations.create("mod").extendsFrom(modInclude, bloated, intransitive);
        Configuration apiInclude = this.configurations.create("apiInclude");

        Classes.reinterpret(bloated.getDependencies(), BloatedDependencySet.klass);
        Classes.reinterpret(bloatedInclude.getDependencies(), BloatedDependencySet.klass);
        Classes.reinterpret(intransitive.getDependencies(), IntransitiveDependencySet.klass);
        Classes.reinterpret(intransitiveInclude.getDependencies(), IntransitiveDependencySet.klass);

        this.configurations.getByName("api").extendsFrom(apiInclude);
        this.configurations.getByName("modApi").extendsFrom(mod);
        this.configurations.getByName("include").extendsFrom(apiInclude, modInclude);

        this.configurations.all(configuration -> configuration.resolutionStrategy(strategy -> {
            strategy.cacheDynamicVersionsFor(0, "seconds");
            strategy.cacheChangingModulesFor(0, "seconds");
        }));
    }
}
