package net.auoeke.wheel.extension;

import org.gradle.api.Project;

public class WheelForgeLoomExtension extends WheelExtension implements WheelForgeExtensionBase, WheelLoomExtensionBase {
    public String yarn;
    public String forge;
    public String genSources;

    public WheelForgeLoomExtension(Project project) {
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

    @Override
    public String yarn() {
        return this.yarn;
    }

    @Override
    public void yarn(String value) {
        this.yarn = value;
    }

    @Override
    public String genSources() {
        return this.genSources;
    }
}
