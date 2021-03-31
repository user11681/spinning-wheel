package user11681.wheel;

import groovy.lang.Closure;
import java.util.Locale;
import org.gradle.api.JavaVersion;
import org.gradle.util.ConfigureUtil;
import user11681.wheel.dependency.DependencyEntry;
import user11681.wheel.dependency.RepositoryContainer;
import user11681.wheel.publish.PublishingConfig;

public class WheelExtension {
    public PublishingConfig publish = new PublishingConfig();

    public boolean nospam = true;

    public String minecraftVersion;
    public String yarnBuild;

    public JavaVersion javaVersion = JavaVersion.VERSION_1_8;

    private static final RepositoryContainer dependencies = new RepositoryContainer((RepositoryContainer dependencies) -> {
        dependencies.repository("auoeke", "https://auoeke.jfrog.io/artifactory/maven")
            .dependency("grossfabrichacks", "net.devtech:grossfabrichacks:latest.release")
            .dependency("unsafe", "net.gudenau.lib:unsafe:latest.release")
            .dependency("bason", "user11681:bason:latest.release")
            .dependency("cell", "user11681:cell:latest.release")
            .dependency("limitless", "user11681:limitless:latest.release")
            .dependency("narratoroff", "user11681:narratoroff:latest.release")
            .dependency("noauth", "user11681:noauth:latest.release")
            .dependency("optional", "user11681:optional:latest.release")
            .dependency("phormat", "user11681:phormat:latest.release")
            .dependency("projectfabrok", "user11681:projectfabrok:latest.release")
            .dependency("prone", "user11681:prone:latest.release")
            .dependency("commonformatting", "user11681:common-formatting:latest.release")
            .dependency("dynamicentry", "user11681:dynamicentry:latest.release")
            .dependency("huntinghamhills", "user11681:fabricasmtools:latest.release")
            .dependency("invisiblelivingentities", "user11681:invisiblelivingentities:latest.release")
            .dependency("reflect", "user11681:reflect:latest.release")
            .dependency("shortcode", "user11681:shortcode:latest.release");
        dependencies.repository("blamejared", "https://maven.blamejared.com");
        dependencies.repository("boundarybreaker", "https://server.bbkr.space/artifactory/libs-release")
            .dependency("cottonresources", "io.github.cottonmc:cotton-resources:latest.release");
        dependencies.repository("buildcraft", "https://mod-buildcraft.com/maven");
        dependencies.repository("central", "https://repo.maven.apache.org/maven2/")
            .dependency("toml4j", "com.moandjiezana.toml:toml4j:latest.release")
            .dependency("junit", "org.junit.jupiter:junit-jupiter:latest.release");
        dependencies.repository("cursemaven", "https://www.cursemaven.com")
            .dependency("aquarius", "curse.maven:aquarius-301299:3132504")
            .dependency("charm", "curse.maven:charm-318872:3140951")
            .dependency("moenchantments", "curse.maven:moenchantments-320806:3084973");
        dependencies.repository("dblsaiko", "https://maven.dblsaiko.net/");
        dependencies.repository("earthcomputer", "https://dl.bintray.com/earthcomputer/mods")
            .dependency("multiconnect", "net.earthcomputer:multiconnect:latest.release:api");
        dependencies.repository("fabric", "https://maven.fabricmc.net")
            .dependency("api", "net.fabricmc.fabric-api:fabric-api:latest.release")
            .dependency("apibase", "net.fabricmc.fabric-api:fabric-api-base:latest.release")
            .dependency("apiblockrenderlayer", "net.fabricmc.fabric-api:fabric-blockrenderlayer-v1:latest.release")
            .dependency("apicommand", "net.fabricmc.fabric-api:fabric-command-api-v1:latest.release")
            .dependency("apiscreenhandler", "net.fabricmc.fabric-api:fabric-screen-handler-api-v1:latest.release")
            .dependency("apieventsinteraction", "net.fabricmc.fabric-api:fabric-events-interaction-v0:latest.release")
            .dependency("apikeybindings", "net.fabricmc.fabric-api:fabric-key-binding-api-v1:latest.release")
            .dependency("apilifecycleevents", "net.fabricmc.fabric-api:fabric-lifecycle-events-v1:latest.release")
            .dependency("apinetworking", "net.fabricmc.fabric-api:fabric-networking-api-v1:latest.release")
            .dependency("apirendererapi", "net.fabricmc.fabric-api:fabric-renderer-api-v1:latest.release")
            .dependency("apirendererindigo", "net.fabricmc.fabric-api:fabric-renderer-indigo:latest.release")
            .dependency("apiresourceloader", "net.fabricmc.fabric-api:fabric-resource-loader-v0:latest.release")
            .dependency("apitagextensions", "net.fabricmc.fabric-api:fabric-tag-extensions-v0:latest.release");
        dependencies.repository("grossfabrichackers", "https://raw.githubusercontent.com/GrossFabricHackers/maven/master");
        dependencies.repository("halfof2", "https://raw.githubusercontent.com/Devan-Kerman/Devan-Repo/master")
            .dependency("arrp", "net.devtech:arrp:latest.release");
        dependencies.repository("jamieswhiteshirt", "https://maven.jamieswhiteshirt.com/libs-release")
            .dependency("reachentityattributes", "com.jamieswhiteshirt:reach-entity-attributes:latest.release");
        dependencies.repository("jcenter", "https//jcenter.bintray.com");
        dependencies.repository("jitpack", "https://jitpack.io")
            .dependency("astromine", "com.github.Chainmail-Studios:Astromine:1.8.1")
            .dependency("fabricasm", "com.github.Chocohead:Fabric-ASM:master-SNAPSHOT")
            .dependency("liltaterreloaded", "com.github.Yoghurt4C:LilTaterReloaded:fabric-1.16-SNAPSHOT");
        dependencies.repository("ladysnake", "https://dl.bintray.com/ladysnake/libs")
            .dependency("cardinalcomponents", "io.github.onyxstudios:Cardinal-Components-API:latest.release")
            .dependency("cardinalcomponentsbase", "io.github.onyxstudios.Cardinal-Components-API:cardinal-components-base:latest.release")
            .dependency("cardinalcomponentsentity", "io.github.onyxstudios.Cardinal-Components-API:cardinal-components-entity:latest.release")
            .dependency("cardinalcomponentsitem", "io.github.onyxstudios.Cardinal-Components-API:cardinal-components-item:latest.release");
        dependencies.repository("shedaniel", "https://maven.shedaniel.me")
            .dependency("autoconfig", "me.sargunvohra.mcmods:autoconfig1u:latest.release")
            .dependency("basicmath", "me.shedaniel.cloth:basic-math:latest.release")
            .dependency("clothconfig", "me.shedaniel.cloth:config-2:latest.release")
            .dependency("rei", "me.shedaniel:RoughlyEnoughItems:latest.release");
        dependencies.repository("terraformers", "https://maven.terraformersmc.com")
            .dependency("modmenu", "com.terraformersmc:modmenu:latest.release");
        dependencies.repository("wrenchable", "https://dl.bintray.com/zundrel/wrenchable");
    });

    private static String sanitize(String key) {
        return key.replaceAll("[_-]", "").toLowerCase(Locale.ROOT);
    }

    public static String repository(String key) {
        return dependencies.repository(sanitize(key));
    }

    public static void repository(String key, String value) {
        dependencies.putRepository(sanitize(key), value);
    }

    public static DependencyEntry dependency(String key) {
        return dependencies.entry(sanitize(key));
    }

    public void publish(Closure<?> closure) {
        ConfigureUtil.configure(closure, this.publish);
    }

    public void setPublish(boolean enabled) {
        this.publish.enabled = enabled;
    }

    public void setJavaVersion(Object version) {
        this.javaVersion = JavaVersion.toVersion(version);
    }
}
