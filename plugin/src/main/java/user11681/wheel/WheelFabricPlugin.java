package user11681.wheel;

import java.io.File;
import java.net.URI;
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
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.SourceSet;
import org.intellij.lang.annotations.Language;
import user11681.reflect.Invoker;
import user11681.uncheck.ThrowingConsumer;
import user11681.uncheck.Uncheck;
import user11681.wheel.extension.Channel;
import user11681.wheel.extension.WheelFabricExtension;
import user11681.wheel.util.ThrowingAction;

public class WheelFabricPlugin extends WheelPlugin<WheelFabricExtension> {
    private static final Map<String, String> latestYarnBuilds = new HashMap<>();

    public LoomGradleExtension loom;
    public NamedDomainObjectContainer<RunConfigSettings> runConfigs;

    private static String meta(String endpoint, @Language("RegExp") String pattern) {
        Matcher matcher = Uncheck.handle(() -> Pattern.compile(pattern).matcher(http().send(
            HttpRequest.newBuilder().GET().uri(new URI("https://meta.fabricmc.net/v2/versions/" + endpoint)).build(),
            HttpResponse.BodyHandlers.ofString()
        ).body()));

        matcher.find();

        return matcher.group();
    }

    @Override
    public void apply(Project project) {
        if (project.getPlugins().hasPlugin("fabric-loom")) {
            throw new IllegalStateException("fabric-loom must be either specified before wheel without being applied or not specified at all.");
        }

        super.apply(project, WheelFabricExtension.class);

        this.loom = this.extensions.getByType(LoomGradleExtension.class);
        // this.loom.shareCaches = false; // shareCaches = true prevents dev JAR remapping for some reason
        this.runConfigs = this.loom.getRunConfigs();
    }

    @Override
    protected String fetchMinecraftVersion() {
        return meta(
            "game",
            this.extension.channel == Channel.RELEASE
                ? "(?<=\"version\": \").*?(?=\",\\s*\"stable\": true)"
                : "(?<=\"version\": \").*?(?=\")"
        );
    }

    @Override
    protected String metadataFile() {
        return "fabric.mod.json";
    }

    @Override
    protected void checkMinecraftVersion() {
        super.checkMinecraftVersion();

        if (this.extension.yarnBuild == null) {
            this.extension.yarnBuild = latestYarnBuilds.computeIfAbsent(this.extension.minecraftVersion, minecraftVersion -> meta(
                "yarn/" + minecraftVersion,
                "(?<=\"build\": )\\d+"
            ));
        }
    }

    @Override
    protected void afterEvaluate() {
        super.afterEvaluate();

        this.logger.lifecycle("Yarn build: {}", this.extension.yarnBuild);

        SourceSet test = this.sourceSet("test");

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

        this.project.afterEvaluate((ThrowingAction<Project>) project -> this.afterLoom());
    }

    @Override
    protected void applyPlugins() {
        super.applyPlugins();

        this.plugins.apply(LoomGradlePlugin.class);
    }

    @Override
    protected void addDependencies() {
        super.addDependencies();

        this.dependencies.add("minecraft", "com.mojang:minecraft:" + this.extension.minecraftVersion);
        this.dependencies.add("mappings", "net.fabricmc:yarn:%s+build.%s:v2".formatted(this.extension.minecraftVersion, this.extension.yarnBuild));
        this.dependencies.add("mod", "net.fabricmc:fabric-loader:latest.release");

        if (this.extension.nospam) {
            this.dependencies.add("mod", "narrator-off");
            this.dependencies.add("mod", "noauth");
        }
    }

    @Override
    protected void configureConfigurations() {
        super.configureConfigurations();

        this.configuration("modApi").extendsFrom(this.configuration("mod"));
    }

    @Override
    protected void configurePublication(MavenPublication publication) {
        super.configurePublication(publication);

        RemapJarTask remapJar = this.task("remapJar");

        publication.artifact(remapJar).builtBy(remapJar);
        publication.artifact(this.task("sourcesJar")).builtBy(this.task("remapSourcesJar"));
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
        if (this.isRoot() && this.extension.run.enabled) {
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

                    exists:
                    if (Files.exists(runPath)) {
                        if (Files.isDirectory(oldPath)) {
                            if (Files.list(oldPath).toList().isEmpty()) {
                                Files.delete(oldPath);

                                break exists;
                            }
                        }

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
}
