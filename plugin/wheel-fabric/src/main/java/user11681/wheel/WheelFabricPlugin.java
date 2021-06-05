package user11681.wheel;

import java.io.File;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.configuration.ide.RunConfigSettings;
import net.fabricmc.loom.task.GenerateSourcesTask;
import net.fabricmc.loom.task.RemapJarTask;
import net.fabricmc.loom.task.RunGameTask;
import net.fabricmc.loom.util.Constants;
import org.gradle.api.JavaVersion;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.SourceSet;
import org.intellij.lang.annotations.Language;
import user11681.reflect.Invoker;
import user11681.uncheck.ThrowingConsumer;
import user11681.wheel.extension.Channel;
import user11681.wheel.extension.WheelFabricExtension;
import user11681.wheel.util.ThrowingAction;

public class WheelFabricPlugin extends WheelPlugin<WheelFabricExtension> {
    private static final Map<String, String> latestYarnBuilds = new HashMap<>();

    private LoomGradleExtension loom;
    private NamedDomainObjectContainer<RunConfigSettings> runConfigs;

    private static String meta(String endpoint, @Language("RegExp") String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(get("https://meta.fabricmc.net/v2/versions/" + endpoint));
        matcher.find();

        return matcher.group();
    }

    @Override
    public void apply(Project project) {
        super.apply(project, "fabric-loom", WheelFabricExtension.class);

        this.loom = this.extensions.getByType(LoomGradleExtension.class);
        // this.loom.shareCaches = false; // shareCaches = true prevents dev JAR remapping for some reason
        this.runConfigs = this.loom.getRunConfigs();
    }

    @Override
    protected String metadataFile() {
        return "fabric.mod.json";
    }

    @Override
    protected void checkMinecraftVersion() {
        if (this.extension.minecraft == null) {
            if (latestMinecraftVersion == null) {
                latestMinecraftVersion = meta(
                    "game",
                    this.extension.channel == Channel.RELEASE
                        ? "(?<=\"version\": \").*?(?=\",\\s*\"stable\": true)"
                        : "(?<=\"version\": \").*?(?=\")"
                );
            }

            this.extension.minecraft = latestMinecraftVersion;
        }

        if (this.extension.yarnBuild == null) {
            this.extension.yarnBuild = latestYarnBuilds.computeIfAbsent(this.extension.minecraft, minecraftVersion -> meta(
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
    protected void addDependencies() {
        super.addDependencies();

        this.dependencies.add(Constants.Configurations.MINECRAFT, "com.mojang:minecraft:" + this.extension.minecraft);
        this.dependencies.add(Constants.Configurations.MAPPINGS, "net.fabricmc:yarn:%s+build.%s:v2".formatted(this.extension.minecraft, this.extension.yarnBuild));
        this.dependencies.add(MOD, "net.fabricmc:fabric-loader:latest.release");

        if (this.extension.nospam) {
            this.dependencies.add(MOD, "narrator-off");
            this.dependencies.add(MOD, "noauth");
        }
    }

    @Override
    protected void configureConfigurations() {
        super.configureConfigurations();

        this.configuration("modApi").extendsFrom(this.configuration(MOD));
    }

    @Override
    protected String compatibilityVersion(Object version) {
        if (version == null) {
            return this.extension.channel == Channel.RELEASE ? "8" : "16";
        }

        return (version.equals("latest") ? JavaVersion.current() : version).toString();
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
            GenerateSourcesTask genSources = this.task(this.extension.genSources);

            if (!((File) Invoker.bind(genSources, "getMappedJarFileWithSuffix", File.class, String.class).invoke("-sources.jar")).exists()) {
                this.logger.lifecycle("Sources not found; running {}.", genSources.getName());

                this.enqueue(genSources);
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
                            if (Files.list(oldPath).count() == 0) {
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
