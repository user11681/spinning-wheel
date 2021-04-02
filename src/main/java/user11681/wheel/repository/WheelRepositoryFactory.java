package user11681.wheel.repository;

import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.internal.artifacts.repositories.DefaultBaseRepositoryFactory;
import user11681.reflect.Classes;

@SuppressWarnings("ConstantConditions")
public class WheelRepositoryFactory extends DefaultBaseRepositoryFactory {
	public static final long classPointer = Classes.getClassPointer(WheelRepositoryFactory.class);

    public WheelRepositoryFactory() {
        super(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Override
    public MavenArtifactRepository createMavenRepository() {
        return Classes.staticCast(super.createMavenRepository(), WheelMavenArtifactRepository.classPointer);
    }
}
