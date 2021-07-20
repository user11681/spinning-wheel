package user11681.wheel.extension;

public class WheelForgeExtension extends WheelExtension implements WheelForgeExtensionBase {
    public String forge;

    @Override
    public String forge() {
        return this.forge;
    }

    @Override
    public void forge(String value) {
        this.forge = value;
    }
}
