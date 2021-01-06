package nz.co.mirality.storage4computercraft.integration.theoneprobe;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import nz.co.mirality.storage4computercraft.RS4CC;
import nz.co.mirality.storage4computercraft.util.Platform;

import static nz.co.mirality.storage4computercraft.RS4CC.TOP_MOD_ID;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = RS4CC.ID)
public class TOP {
    @SubscribeEvent
    public static void enqueueIMC(final InterModEnqueueEvent e) {
        Platform.maybeLoadIntegration(TOP_MOD_ID, "theoneprobe.GotTheOneProbe")
                .ifPresent(handler -> InterModComms.sendTo(TOP_MOD_ID, "getTheOneProbe",
                        () -> handler));
    }
}
