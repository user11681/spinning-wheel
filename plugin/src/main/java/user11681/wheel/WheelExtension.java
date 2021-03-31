package user11681.wheel;

import groovy.lang.Closure;
import org.gradle.api.JavaVersion;
import org.gradle.util.ConfigureUtil;
import user11681.wheel.dependency.Dependency;
import user11681.wheel.dependency.RepositoryContainer;
import user11681.wheel.publish.PublishingConfig;

public class WheelExtension {
    public PublishingConfig publish = new PublishingConfig();

    public boolean nospam = true;

    public String minecraftVersion;
    public String yarnBuild;

    public JavaVersion javaVersion = JavaVersion.VERSION_1_8;

    public static final RepositoryContainer dependencies = new RepositoryContainer((RepositoryContainer dependencies) -> {
        dependencies.repository("auoeke", "https://auoeke.jfrog.io/artifactory/maven")
            .dependency("bason", "user11681:bason:latest.release")
            .dependency("cell", "user11681:cell:latest.release")
            .dependency("common-formatting", "user11681:common-formatting:latest.release")
            .dependency("dynamic-entry", "user11681:dynamicentry:latest.release")
            .dependency("gross-fabric-hacks", "net.devtech:grossfabrichacks:latest.release")
            .dependency("huntingham-hills", "user11681:fabricasmtools:latest.release")
            .dependency("invisible-living-entities", "user11681:invisiblelivingentities:latest.release")
            .dependency("limitless", "user11681:limitless:latest.release")
            .dependency("narrator-off", "user11681:narratoroff:latest.release")
            .dependency("noauth", "user11681:noauth:latest.release")
            .dependency("optional", "user11681:optional:latest.release")
            .dependency("phormat", "user11681:phormat:latest.release")
            .dependency("project-fabrok", "user11681:projectfabrok:latest.release")
            .dependency("prone", "user11681:prone:latest.release")
            .dependency("reflect", "user11681:reflect:latest.release")
            .dependency("shortcode", "user11681:shortcode:latest.release")
            .dependency("unsafe", "net.gudenau.lib:unsafe:latest.release");
        dependencies.repository("blamejared", "https://maven.blamejared.com");
        dependencies.repository("boundarybreaker", "https://server.bbkr.space/artifactory/libs-release")
            .dependency("cotton-resources", "io.github.cottonmc:cotton-resources:latest.release");
        dependencies.repository("buildcraft", "https://mod-buildcraft.com/maven");
        dependencies.repository("central", "https://repo.maven.apache.org/maven2/")
            .dependency("junit", "org.junit.jupiter:junit-jupiter:latest.release")
            .dependency("toml4j", "com.moandjiezana.toml:toml4j:latest.release");
        dependencies.repository("cursemaven", "https://www.cursemaven.com")
            .dependency("aquarius", "curse.maven:aquarius-301299:3132504")
            .dependency("charm", "curse.maven:charm-318872:3140951")
            .dependency("moenchantments", "curse.maven:moenchantments-320806:3084973")
            .dependency("better-slabs", "curse.maven:betterslabs-407645:3158336");
        dependencies.repository("dblsaiko", "https://maven.dblsaiko.net/");
        dependencies.repository("earthcomputer", "https://dl.bintray.com/earthcomputer/mods")
            .dependency("multiconnect", "net.earthcomputer:multiconnect:latest.release:api");
        dependencies.repository("fabric", "https://maven.fabricmc.net")
            .dependency("api", "net.fabricmc.fabric-api:fabric-api:latest.release")
            .dependency("api-base", "net.fabricmc.fabric-api:fabric-api-base:latest.release")
            .dependency("api-block-render-layer", "net.fabricmc.fabric-api:fabric-blockrenderlayer-v1:latest.release")
            .dependency("api-command", "net.fabricmc.fabric-api:fabric-command-api-v1:latest.release")
            .dependency("api-events-interaction", "net.fabricmc.fabric-api:fabric-events-interaction-v0:latest.release")
            .dependency("api-key-bindings", "net.fabricmc.fabric-api:fabric-key-binding-api-v1:latest.release")
            .dependency("api-lifecycle-events", "net.fabricmc.fabric-api:fabric-lifecycle-events-v1:latest.release")
            .dependency("api-networking", "net.fabricmc.fabric-api:fabric-networking-api-v1:latest.release")
            .dependency("api-renderer-api", "net.fabricmc.fabric-api:fabric-renderer-api-v1:latest.release")
            .dependency("api-renderer-indigo", "net.fabricmc.fabric-api:fabric-renderer-indigo:latest.release")
            .dependency("api-resource-loader", "net.fabricmc.fabric-api:fabric-resource-loader-v0:latest.release")
            .dependency("api-screen", "net.fabricmc.fabric-api:fabric-screen-api-v1:latest.release")
            .dependency("api-screen-handler", "net.fabricmc.fabric-api:fabric-screen-handler-api-v1:latest.release")
            .dependency("api-tag-extensions", "net.fabricmc.fabric-api:fabric-tag-extensions-v0:latest.release");
        dependencies.repository("gross-fabric-hackers", "https://raw.githubusercontent.com/GrossFabricHackers/maven/master");
        dependencies.repository("halfof2", "https://raw.githubusercontent.com/Devan-Kerman/Devan-Repo/master")
            .dependency("arrp", "net.devtech:arrp:latest.release");
        dependencies.repository("haven-king", "https://hephaestus.dev/release")
            .dependency("conrad", "dev.inkwell:conrad:latest.release");
        dependencies.repository("jamies-white-shirt", "https://maven.jamieswhiteshirt.com/libs-release")
            .dependency("reach-entity-attributes", "com.jamieswhiteshirt:reach-entity-attributes:latest.release");
        dependencies.repository("jcenter", "https//jcenter.bintray.com");
        dependencies.repository("jitpack", "https://jitpack.io")
            .dependency("astromine", "com.github.Chainmail-Studios:Astromine:1.8.1")
            .dependency("fabric-asm", "com.github.Chocohead:Fabric-ASM:master-SNAPSHOT")
            .dependency("lil-tater-reloaded", "com.github.Yoghurt4C:LilTaterReloaded:fabric-1.16-SNAPSHOT");
        dependencies.repository("ladysnake", "https://dl.bintray.com/ladysnake/libs")
            .dependency("cardinal-components", "io.github.onyxstudios:Cardinal-Components-API:latest.release")
            .dependency("cardinal-components-base", "io.github.onyxstudios.Cardinal-Components-API:cardinal-components-base:latest.release")
            .dependency("cardinal-components-entity", "io.github.onyxstudios.Cardinal-Components-API:cardinal-components-entity:latest.release")
            .dependency("cardinal-components-item", "io.github.onyxstudios.Cardinal-Components-API:cardinal-components-item:latest.release");
        dependencies.repository("shedaniel", "https://maven.shedaniel.me")
            .dependency("auto-config", "me.sargunvohra.mcmods:autoconfig1u:latest.release")
            .dependency("basic-math", "me.shedaniel.cloth:basic-math:latest.release")
            .dependency("cloth-config", "me.shedaniel.cloth:config-2:latest.release")
            .dependency("rei", "me.shedaniel:RoughlyEnoughItems:latest.release");
        dependencies.repository("terraformers", "https://maven.terraformersmc.com")
            .dependency("mod-menu", "com.terraformersmc:modmenu:latest.release");
        dependencies.repository("wrenchable", "https://dl.bintray.com/zundrel/wrenchable");
    });

    public static String repository(String key) {
        return dependencies.repository(key);
    }

    public static void repository(String key, String value) {
        dependencies.putRepository(key, value);
    }

    public static Dependency dependency(String key) {
        return dependencies.entry(key);
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
