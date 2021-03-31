package user11681.wheel.dependency;

import java.util.ArrayList;
import java.util.List;

public class Repository {
    public final String key;
    public final String url;
    public final List<DependencyEntry> dependencies = new ArrayList<>();

    public Repository(String key, String url) {
        this.key = key;
        this.url = url;
    }

    public Repository dependency(String key, String artifact) {
        this.dependencies.add(new DependencyEntry(key, artifact, this.key));

        return this;
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
