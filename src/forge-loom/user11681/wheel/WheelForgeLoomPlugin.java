package user11681.wheel;

import org.gradle.api.Project;
import user11681.wheel.extension.WheelForgeLoomExtension;

public class WheelForgeLoomPlugin extends WheelPlugin<WheelForgeLoomPlugin, WheelForgeLoomExtension> {
    @Override
    public void apply(Project project) {
        super.apply(project, "dev.architectury.loom", new WheelForgeLoomExtension());
    }

    @Override
    protected void checkMinecraftVersion() {

    }

    @Override
    protected String metadataFile() {
        return "mods.toml";
    }
}
