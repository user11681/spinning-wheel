package user11681.wheel.dependency;

import user11681.wheel.WheelExtension;

public class DependencyEntry {
    public final String key;
    public final String artifact;
    public final String repository;

    public DependencyEntry(String key, String artifact, String repository) {
        this.artifact = artifact;
        this.key = key;
        this.repository = repository;
    }

    public String resolveRepository() {
        return WheelExtension.repository(this.repository);
    }
}
