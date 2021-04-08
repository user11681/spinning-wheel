package user11681.wheel.extension.dependency;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Repository {
    public final String key;
    public final String url;
    public final List<Dependency> dependencies = new ArrayList<>();

    public Repository(String key, String url) {
        this.key = key;
        this.url = url;
    }

    public Repository dependency(String key, String module) {
        this.dependencies.add(new Dependency(key, module + ":latest.release", this.key));

        return this;
    }

    public Repository dependency(String key, String artifact, String version) {
        this.dependencies.add(new Dependency(key, artifact + ':' + version, this.key));

        return this;
    }

    @Override
    public String toString() {
        boolean multiline = this.dependencies.size() > 1;
        StringBuilder string = new StringBuilder(this.key).append(": {");

        if (multiline) {
            string.append("\n\t");
        }

        string.append(this.dependencies.stream().map(Dependency::toString).collect(Collectors.joining("\n\t")));

        if (multiline) {
            string.append('\n');
        }

        return string.append('}').toString();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof Repository && this.key.equals(((Repository) obj).key);
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }
}
