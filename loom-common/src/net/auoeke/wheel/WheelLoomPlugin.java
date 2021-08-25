package net.auoeke.wheel;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.auoeke.reflect.Invoker;
import net.auoeke.wheel.extension.WheelExtension;
import net.auoeke.wheel.extension.WheelLoomExtensionBase;
import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import net.fabricmc.loom.configuration.ide.RunConfigSettings;
import net.fabricmc.loom.task.RunGameTask;
import org.codehaus.groovy.runtime.StringGroovyMethods;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.SourceSet;
import org.intellij.lang.annotations.Language;
import user11681.uncheck.ThrowingConsumer;

public interface WheelLoomPlugin<E extends WheelExtension & WheelLoomExtensionBase> extends WheelPluginBase<E> {
    Map<String, String> latestYarnBuilds = new HashMap<>();

    private static void makeOld(Path path) throws IOException {
        String moveOld = path.toString() + "-old";
        Path moveOldPath = Path.of(moveOld + 0);

        for (int i = 1; Files.exists(moveOldPath); i++) {
            moveOldPath = Path.of(moveOld + i);
        }

        Files.move(path, moveOldPath);
    }

    @Override
    default void afterEvaluation() {
        WheelPluginBase.super.afterEvaluation();

        SourceSet test = this.sourceSet("test");

        this.runConfigs().create("testClient", settings -> {
            settings.client();
            settings.source(test);
        });

        this.runConfigs().create("testServer", settings -> {
            settings.server();
            settings.source(test);
        });

        this.runConfigs().all(settings -> settings.setIdeConfigGenerated(true));
    }

    @Override
    default void configureConfigurations() {
        WheelPluginBase.super.configureConfigurations();

        this.configuration("modApi").extendsFrom(this.configuration(MOD));
    }

    @Override
    default void configurePublication(MavenPublication publication) {
        WheelPluginBase.super.configurePublication(publication);

        publication.artifact(this.task("remapJar"));
        publication.artifact(this.task("sourcesJar")).builtBy(this.task("remapSourcesJar"));
    }

    @Override
    default void afterMain() {
        this.generateSources();
        this.setRunDirectory();
    }

    @Override
    default void checkMinecraftVersion() {
        if (this.extension().yarn() == null) {
            this.extension().yarn(latestYarnBuilds.computeIfAbsent(this.extension().minecraft, minecraft -> this.meta(
                "yarn/" + minecraft,
                "(?<=\"build\": )\\d+"
            ).findFirst().orElseThrow().group()));
        }

        this.log("Yarn build: {}", this.extension().yarn());
    }

    default LoomGradleExtension loom() {
        return (LoomGradleExtension) this.extension(LoomGradleExtensionAPI.class);
    }

    default Path cache() {
        return this.loom().getFiles().getUserCache().toPath();
    }

    default NamedDomainObjectContainer<RunConfigSettings> runConfigs() {
        return this.loom().getRunConfigs();
    }

    default RunGameTask runTask(RunConfigSettings configuration) {
        return this.task("run" + StringGroovyMethods.capitalize(configuration.getName()));
    }

    default Stream<MatchResult> meta(String name, @Language("RegExp") String pattern) {
        return Pattern.compile(pattern).matcher(this.get("https://meta.fabricmc.net/v2/versions/" + name)).results();
    }

    default void setRunDirectory() {
        if (this.isRoot() && this.extension().run.enabled) {
            this.runConfigs().stream().map(RunConfigSettings::getRunDir).distinct().map(this.root()::file).map(File::toPath).forEach((ThrowingConsumer<Path>) oldPath -> {
                String customPath = this.extension().run.path;
                Path runPath = customPath == null ? this.cache().resolve("run") : Path.of(customPath);

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

    private File sourcePath() {
        return Invoker.invoke(Invoker.bind(this.task(this.extension().genSources()), "getMappedJarFileWithSuffix", File.class, String.class), "-sources.jar");
    }

    private void generateSources() {
        if (this.extension().genSources() != null && !this.sourcePath().exists()) {
            this.log("Sources not found; executing {}.", this.extension().genSources());
            this.execute(this.extension().genSources());
        }
    }
}
