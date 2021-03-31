package user11681.wheel.dependency;

import java.util.Map;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.internal.artifacts.DefaultDependencyFactory;
import user11681.reflect.Classes;
import user11681.wheel.ProjectHandler;
import user11681.wheel.WheelExtension;

@SuppressWarnings("ConstantConditions")
public class WheelDependencyFactory extends DefaultDependencyFactory {
    public static final long classPointer = Classes.getClassPointer(WheelDependencyFactory.class);

    public WheelDependencyFactory() {
        super(null, null, null, null, null, null);
    }

    private static String changeVersion(String artifact, String version) {
        String[] segments = artifact.split(":");

        segments[2] = version;

        return String.join(":", segments);
    }

    private static void addRepository(String repository) {
        if (ProjectHandler.currentProject != null && repository != null) {
            RepositoryHandler repositories = ProjectHandler.currentProject.getRepositories();

            for (ArtifactRepository artifactRepository : repositories) {
                if (artifactRepository instanceof MavenArtifactRepository && repository.equals(((MavenArtifactRepository) artifactRepository).getUrl().toString())) {
                    return;
                }
            }

            repositories.maven((MavenArtifactRepository artifactRepository) -> artifactRepository.setUrl(repository));
        }
    }

    private static boolean addRepository(Dependency entry) {
        if (entry != null) {
            addRepository(entry.resolveRepository());

            return true;
        }

        return false;
    }

    private static Object resolve(String dependency) {
        if (dependency.startsWith("curse.maven:")) {
            addRepository(WheelExtension.repository("curse-maven"));

            return dependency;
        }

        if (dependency.startsWith("com.github.")) {
            addRepository(WheelExtension.repository("jitpack"));

            return dependency;
        }

        String[] components = dependency.split(":");

        if (components.length == 2) {
            Dependency entry = WheelExtension.dependency(components[0]);

            if (addRepository(entry)) {
                return changeVersion(entry.artifact, components[1]);
            }
        }

        Dependency entry = WheelExtension.dependency(dependency);

        if (addRepository(entry)) {
            return entry.artifact;
        }

        if (ProjectHandler.currentProject != null && ProjectHandler.currentProject.findProject(dependency) != null) {
            return ProjectHandler.currentProject.getDependencies().project(Map.of("path", dependency));
        }

        return dependency;
    }

    @Override
    public org.gradle.api.artifacts.Dependency createDependency(Object dependencyNotation) {
        if (dependencyNotation instanceof String) {
            return super.createDependency(resolve((String) dependencyNotation));
        }

        return super.createDependency(dependencyNotation);
    }
}
