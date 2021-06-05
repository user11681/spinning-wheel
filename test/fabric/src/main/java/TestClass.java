import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;

public class TestClass implements ModInitializer {
    @Override
    public void onInitialize() {
        System.exit(0);
    }

    public static class Client implements ClientModInitializer {
        @Override
        public void onInitializeClient() {}
    }
}
