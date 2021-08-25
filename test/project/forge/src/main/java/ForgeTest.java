import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@Mod("test")
@EventBusSubscriber()
public class ForgeTest {
    @SubscribeEvent
    public static void cancelDeath(LivingDeathEvent event) {
        event.setCanceled(true);
    }
}
