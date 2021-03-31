package user11681.wheel.publish;

import groovy.lang.Closure;
import org.gradle.util.ConfigureUtil;

public class PublishingConfig {
    public final ExternalRepositoryConfig external = new ExternalRepositoryConfig();
    public final PublicationConfig publication = new PublicationConfig();

    public boolean enabled = true;
    public boolean local = true;

    public void external(Closure<?> closure) {
        ConfigureUtil.configure(closure, this.external);
    }

    public void publication(Closure<?> closure) {
        ConfigureUtil.configure(closure, this.publication);
    }
}
