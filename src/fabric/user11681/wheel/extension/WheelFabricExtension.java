package user11681.wheel.extension;

import java.util.Locale;

public class WheelFabricExtension extends WheelExtension {
    public String yarnBuild;
    public String genSources = "genSources";
    public Channel channel = Channel.RELEASE;
    public boolean nospam = true;

    public void setChannel(Object channel) {
        this.channel = Channel.valueOf(String.valueOf(channel).toUpperCase(Locale.ROOT));
    }
}
