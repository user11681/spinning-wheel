package net.auoeke.wheel.extension.dependency;

import java.util.Objects;
import net.auoeke.wheel.extension.WheelExtension;

public class Dependency {
    public final String key;
    public final String artifact;
    public final String repository;

    public Dependency(String key, String artifact, String repository) {
        this.key = key;
        this.artifact = artifact;
        this.repository = repository;
    }

    @Override
    public String toString() {
        return "%s (%s)".formatted(this.key, this.artifact);
    }

    public String resolveRepository() {
        return WheelExtension.repository(this.repository);
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if (object instanceof Dependency) {
            Dependency that = (Dependency) object;

            return Objects.equals(this.key, that.key)
                && Objects.equals(this.artifact, that.artifact)
                && Objects.equals(this.repository, that.repository);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.key, this.artifact, this.repository);
    }
}
