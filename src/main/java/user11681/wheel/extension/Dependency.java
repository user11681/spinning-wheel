package user11681.wheel.extension;

import user11681.wheel.extension.WheelExtension;

public class Dependency {
    public final String key;
    public final String artifact;
    public final String repository;

    public Dependency(String key, String artifact, String repository) {
        this.artifact = artifact;
        this.key = key;
        this.repository = repository;
    }

    @Override
    public String toString() {
        return "%s (%s)".formatted(this.key, this.artifact);
    }

    public String resolveRepository() {
        return WheelExtension.repository(this.repository);
    }
}
