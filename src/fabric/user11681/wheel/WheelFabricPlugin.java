package user11681.wheel;

import net.fabricmc.loom.util.Constants;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;
import user11681.wheel.extension.Channel;
import user11681.wheel.extension.WheelFabricExtension;

public class WheelFabricPlugin extends WheelPlugin<WheelFabricPlugin, WheelFabricExtension> implements WheelLoomPlugin<WheelFabricExtension> {
    @Override
    public void apply(@NotNull Project project) {
        super.apply(project, "fabric-loom", new WheelFabricExtension());
    }

    @Override
    public String metadataFile() {
        return "fabric.mod.json";
    }

    @Override
    public void checkMinecraftVersion() {
        if (this.extension.minecraft == null) {
            if (latestMinecraftVersion == null) {
                latestMinecraftVersion = this.meta(
                    "game",
                    this.extension.channel == Channel.RELEASE
                        ? "(?<=\"version\": \").*?(?=\",\\s*\"stable\": true)"
                        : "(?<=\"version\": \").*?(?=\")"
                ).filter(result -> !result.group().contains("experiment")).findFirst().orElseThrow().group();
            }

            this.extension.minecraft = latestMinecraftVersion;
        }

        WheelLoomPlugin.super.checkMinecraftVersion();
    }

    @Override
    public void addDependencies() {
        super.addDependencies();

        this.dependency(Constants.Configurations.MINECRAFT, "com.mojang:minecraft:" + this.extension.minecraft);
        this.dependency(Constants.Configurations.MAPPINGS, "net.fabricmc:yarn:%s+build.%s:v2".formatted(this.extension.minecraft, this.extension.yarn));
        this.dependency(MOD, "net.fabricmc:fabric-loader:latest.integration");

        if (this.extension.nospam) {
            this.dependency(MOD, "narrator-off");
            this.dependency(MOD, "noauth");
        }
    }

    @Override
    public String defaultJavaVersion() {
        return "16";
    }
}
