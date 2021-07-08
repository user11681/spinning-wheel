package user11681.wheel.extension.publish;

import org.gradle.api.Action;

public class PublishingConfig {
    public final ExternalRepositoryConfig external = new ExternalRepositoryConfig();
    public final PublicationConfig publication = new PublicationConfig();

    public boolean enabled = true;
    public boolean local = true;

    public void external(Action<ExternalRepositoryConfig> action) {
        action.execute(this.external);
    }

    public void publication(Action<PublicationConfig> action) {
        action.execute(this.publication);
    }

    public void setPublication(boolean enabled) {
        this.publication.enabled = enabled;
    }
}
