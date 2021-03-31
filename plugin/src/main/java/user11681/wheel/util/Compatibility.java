package user11681.wheel.util;

import org.gradle.api.JavaVersion;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.JavaPluginConvention;

public class Compatibility {
    private final Convention convention;

    public Compatibility(Convention convention) {
        this.convention = convention;

        this.setSource(8);
        this.setTarget(8);
    }

    public JavaVersion getSource() {
        return this.convention().getSourceCompatibility();
    }

    public void setSource(Object source) {
        this.convention().setSourceCompatibility(source);
    }

    public JavaVersion getTarget() {
        return this.convention().getTargetCompatibility();
    }

    public void setTarget(Object target) {
        this.convention().setTargetCompatibility(target);
    }

    private JavaPluginConvention convention() {
        return this.convention.getPlugin(JavaPluginConvention.class);
    }
}
