package user11681.wheel.dependency;

import java.util.HashMap;
import java.util.Map;
import org.gradle.api.Action;
import user11681.wheel.extension.dependency.Dependency;
import user11681.wheel.extension.dependency.Repository;
import user11681.wheel.util.Util;

public class RepositoryContainer {
    public final Map<String, Repository> repositories = new HashMap<>();

    public RepositoryContainer configure(Action<RepositoryContainer> action) {
        action.execute(this);

        return this;
    }

    public String repository(String key) {
        Repository repository = this.repositories.get(Util.sanitize(key));

        return repository != null ? repository.url : null;
    }

    public Repository repository(String key, String url) {
        return this.repositories.computeIfAbsent(Util.sanitize(key), key2 -> new Repository(key2, url));
    }

    public void putRepository(String key, String url) {
        key = Util.sanitize(key);

        this.repositories.put(key, new Repository(key, url));
    }

    public Dependency entry(String dependency) {
        String sanitizedDependency = Util.sanitize(dependency);

        return this.repositories.values().stream()
            .flatMap(repository -> repository.dependencies.stream())
            .filter(entry -> Util.sanitize(entry.key).equals(sanitizedDependency) || Util.sanitize(entry.artifact.split(":", 3)[1]).equals(sanitizedDependency))
            .findFirst().orElse(null);
    }
}
