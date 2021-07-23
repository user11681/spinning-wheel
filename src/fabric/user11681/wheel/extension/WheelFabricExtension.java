package user11681.wheel.extension;

import java.util.Locale;
import org.gradle.api.Project;

public class WheelFabricExtension extends WheelExtension implements WheelLoomExtensionBase {
    public String yarn;
    public String genSources = "genSources";
    public Channel channel = Channel.RELEASE;
    public boolean nospam = true;

    public WheelFabricExtension(Project project) {
        super(project);
    }

    public void setChannel(Object channel) {
        this.channel = Channel.valueOf(String.valueOf(channel).toUpperCase(Locale.ROOT));
    }

    @Override
    public String yarn() {
        return this.yarn;
    }

    @Override
    public void yarn(String value) {
        this.yarn = value;
    }

    @Override
    public String genSources() {
        return this.genSources;
    }
}
