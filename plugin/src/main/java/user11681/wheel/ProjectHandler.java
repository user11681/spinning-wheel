/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016, 2017, 2018 FabricMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package user11681.wheel;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.LoomGradlePlugin;
import net.fabricmc.loom.configuration.ide.RunConfigSettings;
import net.fabricmc.loom.task.RunGameTask;
import net.gudenau.lib.unsafe.Unsafe;
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
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.plugins.PluginManager;
import org.gradle.api.plugins.UnknownPluginException;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.tasks.Jar;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.intellij.lang.annotations.Language;
import user11681.reflect.Accessor;
import user11681.reflect.Classes;
import user11681.wheel.dependency.WheelDependencyFactory;
import user11681.wheel.dependency.configuration.BloatedDependencySet;
import user11681.wheel.dependency.configuration.IntransitiveDependencySet;
import user11681.wheel.repository.WheelRepositoryFactory;

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
        this.extension = this.extensions.create("wheel", WheelExtension.class, this.convention);
    }

    private static String sendGET(String uri, @Language("RegExp") String pattern, Function<String, String> thing) {
        if (httpClient == null) {
            httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
        }

        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(uri)).build();

        try {
            Matcher matcher = Pattern.compile(pattern).matcher(thing.apply(httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body()));

            matcher.find();

            return matcher.group();
        } catch (Throwable throwable) {
            throw Unsafe.throwException(throwable);
        }
    }

    private void checkMinecraftVersion() {
        if (this.extension.minecraftVersion == null) {
            if (latestMinecraftVersion == null) {
                latestMinecraftVersion = sendGET("https://meta.fabricmc.net/v2/versions/game", "(?<=\").*(?=\")", (String body) -> body.substring(body.indexOf(":") + 1));
            }

            this.extension.minecraftVersion = latestMinecraftVersion;
        }
    }

    private void checkYarnBuild() {
        if (this.extension.yarnBuild == null) {
            if (latestYarnBuilds.get(extension.minecraftVersion) == null) {
                latestYarnBuilds.put(extension.minecraftVersion, sendGET(
                    "https://meta.fabricmc.net/v2/versions/yarn/" + extension.minecraftVersion,
                    "\\d+",
                    (String body) -> body.substring(body.indexOf("separator"))
                ));
            }

            this.extension.yarnBuild = latestYarnBuilds.get(extension.minecraftVersion);
        }
    }

    public void handle() {
        currentProject = this.project;

        this.plugins.apply(JavaLibraryPlugin.class);
        this.plugins.apply(MavenPublishPlugin.class);

        Classes.staticCast(Accessor.getObject(this.repositories, "repositoryFactory"), WheelRepositoryFactory.classPointer);
        Classes.staticCast(Accessor.getObject(this.dependencies, "dependencyFactory"), WheelDependencyFactory.classPointer);

//        this.configurations.create("dev");

        this.project.beforeEvaluate(ignored -> this.beforeEvaluate());
        this.project.afterEvaluate(ignored -> this.afterEvaluate());

        try {
            this.plugins.apply("fabric-loom");
        } catch (UnknownPluginException exception) {
            this.plugins.apply(LoomGradlePlugin.class);
        }

        this.extension.javaVersion.setSource(8);
        this.extension.javaVersion.setTarget(8);

        this.repositories.mavenLocal();

        this.configureConfigurations();
    }

    private void beforeEvaluate() {
        currentProject = this.project;
    }

    private void afterEvaluate() {
        this.loom = this.extensions.getByType(LoomGradleExtension.class);
        this.runConfigs = this.loom.getRunConfigs();

        this.checkMinecraftVersion();
        this.checkYarnBuild();

        this.dependencies.add("minecraft", "com.mojang:minecraft:" + this.extension.minecraftVersion);
        this.dependencies.add("mappings", "net.fabricmc:yarn:%s+build.%s:v2".formatted(this.extension.minecraftVersion, this.extension.yarnBuild));
        this.dependencies.add("mod", "net.fabricmc:fabric-loader:latest.release");
        this.dependencies.add("testImplementation", "org.junit.jupiter:junit-jupiter:latest.release");

        if (this.extension.nospam) {
            this.dependencies.add("modApi", "narrator-off");
            this.dependencies.add("modApi", "noauth");
        }

        this.extensions.getByType(JavaPluginExtension.class).withSourcesJar();

        this.tasks.withType(JavaCompile.class).forEach((JavaCompile task) -> {
            task.getOptions().setEncoding("UTF-8");
        });

        this.tasks.withType(Jar.class).forEach((Jar task) -> {
//            task.getArchiveClassifier().set("dev");

            task.from("LICENSE");
        });

        final Task remapJar = this.tasks.getByName("remapJar");

        remapJar.doLast((Task remapTask) -> remapTask.getInputs().getFiles().filter((File file) -> file.getName().endsWith(".jar")).forEach(File::delete));

        ProcessResources processResources = (ProcessResources) this.tasks.getByName("processResources");
        processResources.getInputs().property("version", this.project.getVersion());
        processResources.filesMatching("fabric.mod.json", (FileCopyDetails details) -> details.expand(new HashMap<>(Map.of("version", project.getVersion()))));

//        File devJar = project.file(String.format("%s/libs/%s-%s-dev.jar", this.project.getBuildDir(), this.project.getName(), this.project.getVersion()));

//        this.artifacts.add("dev", devJar, (ConfigurablePublishArtifact artifact) -> artifact.builtBy(tasks.getByName("jar")).setType("jar"));

//        if (devJar.exists()) {
//            RemapJarTask task = (RemapJarTask) tasks.getByName("remapJar");
//
//            task.getInput().set(devJar);
//            task.getArchiveFileName().set(String.format("%s-%s.jar", project.getName(), project.getVersion()));
//        }

        FileCollection testClasspath = this.convention.getPlugin(JavaPluginConvention.class).getSourceSets().getByName("test").getRuntimeClasspath();

        this.runConfigs.create("testClient", RunConfigSettings::client);
        this.runConfigs.create("testServer", RunConfigSettings::client);
        ((RunGameTask) this.tasks.getByName("runTestClient")).classpath(testClasspath);
        ((RunGameTask) this.tasks.getByName("runTestServer")).classpath(testClasspath);

        if (this.extension.publish.enabled) {
            PublishingExtension publishing = this.extensions.getByType(PublishingExtension.class);

            if (this.extension.publish.publication.enabled) {
                publishing.getPublications().create(this.extension.publish.publication.name, MavenPublication.class, (MavenPublication publication) -> {
                    publication.setGroupId(String.valueOf(this.project.getGroup()));
                    publication.setArtifactId(this.project.getName());
                    publication.setVersion(String.valueOf(this.project.getVersion()));

                    publication.artifact(remapJar).builtBy(remapJar);
                    publication.artifact(this.tasks.getByName("sourcesJar")).builtBy(this.tasks.getByName("remapSourcesJar"));
                });
            }

            if (this.extension.publish.local) {
                publishing.getRepositories().mavenLocal();
            }

            if (this.extension.publish.external.enabled) {
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

    private void configureConfigurations() {
        Configuration intransitiveInclude = this.configurations.create("intransitiveInclude").setTransitive(false);
        Configuration intransitive = this.configurations.create("intransitive").extendsFrom(intransitiveInclude).setTransitive(false);
        Configuration bloatedInclude = this.configurations.create("bloatedInclude");
        Configuration bloated = this.configurations.create("bloated").extendsFrom(bloatedInclude);
        Configuration modInclude = this.configurations.create("modInclude").extendsFrom(bloatedInclude, intransitiveInclude);
        Configuration mod = this.configurations.create("mod").extendsFrom(modInclude, bloated, intransitive);
        Configuration apiInclude = this.configurations.create("apiInclude");

        Classes.staticCast(Accessor.getObject(bloated, "dependencies"), BloatedDependencySet.classPointer);
        Classes.staticCast(Accessor.getObject(bloatedInclude, "dependencies"), BloatedDependencySet.classPointer);
        Classes.staticCast(Accessor.getObject(intransitive, "dependencies"), IntransitiveDependencySet.classPointer);
        Classes.staticCast(Accessor.getObject(intransitiveInclude, "dependencies"), IntransitiveDependencySet.classPointer);

        this.configurations.getByName("api").extendsFrom(apiInclude);
        this.configurations.getByName("modApi").extendsFrom(mod);
        this.configurations.getByName("include").extendsFrom(apiInclude, modInclude);
    }
}