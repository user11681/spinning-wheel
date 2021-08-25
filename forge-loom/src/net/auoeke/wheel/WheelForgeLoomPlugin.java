package net.auoeke.wheel;

import net.auoeke.wheel.extension.WheelForgeLoomExtension;
import net.fabricmc.loom.util.Constants;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

public class WheelForgeLoomPlugin extends AbstractWheelForgePlugin<WheelForgeLoomPlugin, WheelForgeLoomExtension> implements WheelLoomPlugin<WheelForgeLoomExtension> {
    @Override
    public void apply(@NotNull Project project) {
        super.apply(project, "dev.architectury.loom", new WheelForgeLoomExtension(project));
    }

    @Override
    protected void beforeMain() {
        this.extra().set("loom.platform", "forge");
        this.extra().set("loom.forge.include", "true");
    }

    @Override
    protected void addRepositories() {
        super.addRepositories();

        this.repository("https://maven.architectury.dev");
    }

    @Override
    public void checkMinecraftVersion() {
        super.checkMinecraftVersion();
        WheelLoomPlugin.super.checkMinecraftVersion();
    }

    @Override
    public void addDependencies() {
        super.addDependencies();

        this.dependency(Constants.Configurations.MINECRAFT, "com.mojang:minecraft:" + this.extension.minecraft);
        this.dependency(Constants.Configurations.MAPPINGS, "net.fabricmc:yarn:%s+build.%s:v2".formatted(this.extension.minecraft, this.extension.yarn));
        this.dependency(Constants.Configurations.FORGE, "net.minecraftforge:forge:%s-%s".formatted(this.extension.minecraft, this.extension.forge));
    }
}
