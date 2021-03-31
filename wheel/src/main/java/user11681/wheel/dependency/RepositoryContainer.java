package user11681.wheel.dependency;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class RepositoryContainer {
    public final Map<String, Repository> entries = new HashMap<>();

    public RepositoryContainer(Consumer<RepositoryContainer> initializer) {
        initializer.accept(this);
    }

    public String repository(String key) {
        Repository repository = this.entries.get(key);

        return repository != null ? repository.url : null;
    }

    public Repository repository(String key, String url) {
        return this.entries.computeIfAbsent(key, (String key2) -> new Repository(key2, url));
    }

    public void putRepository(String key, String url) {
        this.entries.put(key, new Repository(key, url));
    }

    public DependencyEntry entry(String key) {
        return this.entries.values().stream()
            .flatMap((Repository repository) -> repository.dependencies.stream())
            .filter((DependencyEntry entry) -> entry.key.equals(key) || entry.artifact.contains(key))
            .findFirst().orElse(null);
    }
}
