package user11681.wheel.extension;

import org.gradle.api.JavaVersion;
import org.gradle.api.tasks.compile.JavaCompile;
import user11681.wheel.ProjectHandler;

public class Compatibility {
    private final ProjectHandler handler;

    public Compatibility(ProjectHandler handler) {
        this.handler = handler;
    }

    public String getSource() {
        return this.compileTask().getSourceCompatibility();
    }

    public void setSource(Object source) {
        this.compileTask().setSourceCompatibility(source == null ? JavaVersion.current().toString() : String.valueOf(source));
    }

    public String getTarget() {
        return this.compileTask().getTargetCompatibility();
    }

    public void setTarget(Object target) {
        this.compileTask().setTargetCompatibility(target == null ? JavaVersion.current().toString() : String.valueOf(target));
    }

    private JavaCompile compileTask() {
        return (JavaCompile) this.handler.tasks.getByName("compileJava");
    }
}
