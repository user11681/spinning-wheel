package user11681.wheel.extension;

import java.util.Locale;
import user11681.wheel.WheelFabricPlugin;

public class WheelFabricExtension extends WheelExtension<WheelFabricExtension, WheelFabricPlugin> {
    public String yarnBuild;
    public String genSources = "genSources";
    public Channel channel = Channel.RELEASE;
    public boolean nospam = true;

    public WheelFabricExtension(WheelFabricPlugin plugin) {
        super(plugin);
    }

    public void setChannel(Object channel) {
        this.channel = Channel.valueOf(String.valueOf(channel).toUpperCase(Locale.ROOT));
    }
}
