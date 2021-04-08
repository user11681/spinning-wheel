package user11681.wheel.repository;

import org.gradle.api.internal.artifacts.repositories.DefaultMavenArtifactRepository;
import user11681.reflect.Classes;
import user11681.wheel.extension.WheelExtension;

@SuppressWarnings("ConstantConditions")
public class WheelMavenArtifactRepository extends DefaultMavenArtifactRepository {
    public static final long classPointer = Classes.klass(WheelMavenArtifactRepository.class);

    public WheelMavenArtifactRepository() {
        super(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Override
    public void setName(String name) {
        super.setName(name);

        String resolvedURL = WheelExtension.repository(name);

        if (resolvedURL != null) {
            this.setUrl(resolvedURL);
        }
    }
}
