package user11681.wheel.extension.dependency;

import user11681.wheel.extension.WheelExtension;

public record Dependency(String key, String artifact, String repository) {
    @Override
    public String toString() {
        return "%s (%s)".formatted(this.key, this.artifact);
    }

    public String resolveRepository() {
        return WheelExtension.repository(this.repository);
    }
}
