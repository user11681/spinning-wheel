package user11681.wheel;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class WheelPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        if (project.getPlugins().hasPlugin("fabric-loom")) {
            throw new IllegalStateException("fabric-loom must be either specified before wheel without being applied not specified at all.");
        }

        new ProjectHandler(project).handle();
    }
}
