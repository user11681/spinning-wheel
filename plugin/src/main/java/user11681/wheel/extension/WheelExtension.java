package user11681.wheel.extension;

import groovy.lang.Closure;
import java.util.Locale;
import org.gradle.util.ConfigureUtil;
import user11681.wheel.dependency.RepositoryContainer;
import user11681.wheel.extension.dependency.Dependency;
import user11681.wheel.extension.publish.PublishingConfig;

public abstract class WheelExtension {
    public boolean clean = true;
    public String genSources = "genSources";
    public String minecraftVersion;
    public Compatibility java;
    public PublishingConfig publish = new PublishingConfig();
    public RunDirectory run = new RunDirectory();
    public Channel channel = Channel.RELEASE;

    public WheelExtension() {
        this.java = new Compatibility();
    }

    public static final RepositoryContainer repositories = new RepositoryContainer().configure((RepositoryContainer dependencies) -> {
        dependencies.repository("auoeke", "https://auoeke.jfrog.io/artifactory/maven")
            .dependency("bason", "user11681:bason")
            .dependency("cell", "user11681:cell")
            .dependency("common-formatting", "user11681:common-formatting")
            .dependency("dynamic-entry", "user11681:dynamicentry")
            .dependency("gross-fabric-hacks", "net.devtech:grossfabrichacks")
            .dependency("huntingham-hills", "user11681:fabricasmtools")
            .dependency("invisible-living-entities", "user11681:invisiblelivingentities")
            .dependency("javac-plugin", "user11681:javac-plugin")
            .dependency("limitless", "user11681:limitless")
            .dependency("narrator-off", "user11681:narrator-off")
            .dependency("noauth", "user11681:noauth")
            .dependency("optional", "user11681:optional")
            .dependency("phormat", "user11681:phormat")
            .dependency("project-fabrok", "user11681:projectfabrok")
            .dependency("prone", "user11681:prone")
            .dependency("reflect", "user11681:reflect")
            .dependency("shortcode", "user11681:shortcode")
            .dependency("uncheck", "user11681:uncheck")
            .dependency("unsafe", "net.gudenau.lib:unsafe");
        dependencies.repository("blamejared", "https://maven.blamejared.com");
        dependencies.repository("boundarybreaker", "https://server.bbkr.space/artifactory/libs-release")
            .dependency("cotton-resources", "io.github.cottonmc:cotton-resources");
        dependencies.repository("buildcraft", "https://mod-buildcraft.com/maven");
        dependencies.repository("central", "https://repo.maven.apache.org/maven2/")
            .dependency("jabel", "com.github.bsideup.jabel:jabel-javac-plugin")
            .dependency("junit", "org.junit.jupiter:junit-jupiter")
            .dependency("toml4j", "com.moandjiezana.toml:toml4j");
        dependencies.repository("cursemaven", "https://www.cursemaven.com")
            .dependency("aquarius", "curse.maven:aquarius-301299", "3132504")
            .dependency("charm", "curse.maven:charm-318872", "3140951")
            .dependency("moenchantments", "curse.maven:moenchantments-320806", "3084973")
            .dependency("better-slabs", "curse.maven:betterslabs-407645", "3158336");
        dependencies.repository("dblsaiko", "https://maven.dblsaiko.net/");
        dependencies.repository("earthcomputer", "https://dl.bintray.com/earthcomputer/mods")
            .dependency("multiconnect", "net.earthcomputer:multiconnect:api");
        dependencies.repository("fabric", "https://maven.fabricmc.net")
            .dependency("api", "net.fabricmc.fabric-api:fabric-api")
            .dependency("api-base", "net.fabricmc.fabric-api:fabric-api-base")
            .dependency("api-block-render-layer", "net.fabricmc.fabric-api:fabric-blockrenderlayer-v1")
            .dependency("api-command", "net.fabricmc.fabric-api:fabric-command-api-v1")
            .dependency("api-events-interaction", "net.fabricmc.fabric-api:fabric-events-interaction-v0")
            .dependency("api-key-bindings", "net.fabricmc.fabric-api:fabric-key-binding-api-v1")
            .dependency("api-lifecycle-events", "net.fabricmc.fabric-api:fabric-lifecycle-events-v1")
            .dependency("api-networking", "net.fabricmc.fabric-api:fabric-networking-api-v1")
            .dependency("api-registry-sync", "net.fabricmc.fabric-api:fabric-registry-sync-v0")
            .dependency("api-renderer-api", "net.fabricmc.fabric-api:fabric-renderer-api-v1")
            .dependency("api-renderer-indigo", "net.fabricmc.fabric-api:fabric-renderer-indigo")
            .dependency("api-resource-loader", "net.fabricmc.fabric-api:fabric-resource-loader-v0")
            .dependency("api-screen", "net.fabricmc.fabric-api:fabric-screen-api-v1")
            .dependency("api-screen-handler", "net.fabricmc.fabric-api:fabric-screen-handler-api-v1")
            .dependency("api-tag-extensions", "net.fabricmc.fabric-api:fabric-tag-extensions-v0");
        dependencies.repository("gross-fabric-hackers", "https://raw.githubusercontent.com/GrossFabricHackers/maven/master");
        dependencies.repository("halfof2", "https://raw.githubusercontent.com/Devan-Kerman/Devan-Repo/master")
            .dependency("arrp", "net.devtech:arrp");
        dependencies.repository("haven-king", "https://hephaestus.dev/release")
            .dependency("conrad", "dev.inkwell:conrad");
        dependencies.repository("jamies-white-shirt", "https://maven.jamieswhiteshirt.com/libs-release")
            .dependency("reach-entity-attributes", "com.jamieswhiteshirt:reach-entity-attributes");
        dependencies.repository("jcenter", "https//jcenter.bintray.com");
        dependencies.repository("jitpack", "https://jitpack.io")
            .dependency("astromine", "com.github.Chainmail-Studios:Astromine", "1.8.1")
            .dependency("fabric-asm", "com.github.Chocohead:Fabric-ASM", "master-SNAPSHOT")
            .dependency("lil-tater-reloaded", "com.github.Yoghurt4C:LilTaterReloaded", "fabric-1.16-SNAPSHOT");
        dependencies.repository("ladysnake", "https://dl.bintray.com/ladysnake/libs")
            .dependency("cardinal-components", "io.github.onyxstudios:Cardinal-Components-API")
            .dependency("cardinal-components-base", "io.github.onyxstudios.Cardinal-Components-API:cardinal-components-base")
            .dependency("cardinal-components-entity", "io.github.onyxstudios.Cardinal-Components-API:cardinal-components-entity")
            .dependency("cardinal-components-item", "io.github.onyxstudios.Cardinal-Components-API:cardinal-components-item");
        dependencies.repository("shedaniel", "https://maven.shedaniel.me")
            .dependency("auto-config", "me.sargunvohra.mcmods:autoconfig1u")
            .dependency("basic-math", "me.shedaniel.cloth:basic-math")
            .dependency("cloth-config", "me.shedaniel.cloth:config-2")
            .dependency("rei", "me.shedaniel:RoughlyEnoughItems");
        dependencies.repository("terraformers", "https://maven.terraformersmc.com")
            .dependency("mod-menu", "com.terraformersmc:modmenu", "1.16+");
        dependencies.repository("wrenchable", "https://dl.bintray.com/zundrel/wrenchable");
    });

    public static String repository(String key) {
        return repositories.repository(key);
    }

    public static void repository(String key, String value) {
        repositories.putRepository(key, value);
    }

    public static Dependency dependency(String key) {
        return repositories.entry(key);
    }

    public void publish(Closure<?> closure) {
        ConfigureUtil.configure(closure, this.publish);
    }

    public void setPublish(boolean enabled) {
        this.publish.enabled = enabled;
    }

    public void java(Closure<?> closure) {
        ConfigureUtil.configure(closure, this.java);
    }

    public void setJava(Object version) {
        this.java.setSource(version);
        this.java.setTarget(version);
    }

    public void setChannel(Object channel) {
        this.channel = Channel.valueOf(String.valueOf(channel).toUpperCase(Locale.ROOT));
    }
}
