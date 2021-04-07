package user11681.wheel.extension;

import org.gradle.api.JavaVersion;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.JavaPluginConvention;

public class Compatibility {
    private final Convention convention;

    public Compatibility(Convention convention) {
        this.convention = convention;
    }

    public JavaVersion getSource() {
        return this.convention().getSourceCompatibility();
    }

    public void setSource(Object source) {
        this.convention().setSourceCompatibility(source == null ? JavaVersion.current() : source);
    }

    public JavaVersion getTarget() {
        return this.convention().getTargetCompatibility();
    }

    public void setTarget(Object target) {
        this.convention().setTargetCompatibility(target == null ? JavaVersion.current() : target);
    }

    private JavaPluginConvention convention() {
        return this.convention.getPlugin(JavaPluginConvention.class);
    }
}
