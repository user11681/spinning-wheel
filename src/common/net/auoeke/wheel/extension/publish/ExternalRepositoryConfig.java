package net.auoeke.wheel.extension.publish;

public class ExternalRepositoryConfig {
    public Object repository = System.getProperty("wheel.repository");
    public String username = System.getProperty("wheel.username");
    public String password = System.getProperty("wheel.password");
}
