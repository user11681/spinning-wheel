import net.fabricmc.api.ModInitializer;

public class TestClass implements ModInitializer {
    @Override
    public void onInitialize() {
        System.out.println("you've been gnomed");

        System.exit(0);
    }
}
