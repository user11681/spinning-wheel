package user11681.wheel.extension.publish;

public class ExternalRepositoryConfig {
    public boolean enabled = true;

    public Object repository = "https://auoeke.jfrog.io/artifactory/maven";
    public String username = System.getenv("ARTIFACTORY_USERNAME");
    public String password = System.getenv("ARTIFACTORY_PASSWORD");
}
