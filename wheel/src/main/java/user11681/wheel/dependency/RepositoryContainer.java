package user11681.wheel.dependency;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class RepositoryContainer {
    public final Map<String, Repository> entries = new HashMap<>();

    public RepositoryContainer(Consumer<RepositoryContainer> initializer) {
        initializer.accept(this);
    }

    private static String sanitize(String key) {
        return key.replaceAll("[_-]", "").toLowerCase(Locale.ROOT);
    }

    public String repository(String key) {
        Repository repository = this.entries.get(sanitize(key));

        return repository != null ? repository.url : null;
    }

    public Repository repository(String key, String url) {
        return this.entries.computeIfAbsent(sanitize(key), (String key2) -> new Repository(key2, url));
    }

    public void putRepository(String key, String url) {
        key = sanitize(key);

        this.entries.put(key, new Repository(key, url));
    }

    public DependencyEntry entry(String dependency) {
        String sanitizedDependency = sanitize(dependency);

        return this.entries.values().stream()
            .flatMap((Repository repository) -> repository.dependencies.stream())
            .filter((DependencyEntry entry) -> entry.key.equals(sanitizedDependency) || entry.artifact.contains(sanitizedDependency))
            .findFirst().orElse(null);
    }
}
