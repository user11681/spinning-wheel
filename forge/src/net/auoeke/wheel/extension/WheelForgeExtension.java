package net.auoeke.wheel.extension;

import org.gradle.api.Project;

public class WheelForgeExtension extends WheelExtension implements WheelForgeExtensionBase {
    public String forge;

    public WheelForgeExtension(Project project) {
        super(project);
    }

    @Override
    public String forge() {
        return this.forge;
    }

    @Override
    public void forge(String value) {
        this.forge = value;
    }
}
