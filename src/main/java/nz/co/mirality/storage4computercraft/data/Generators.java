package nz.co.mirality.storage4computercraft.data;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;
import nz.co.mirality.storage4computercraft.RS4CC;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = RS4CC.ID)
public class Generators {
    @SubscribeEvent
    public static void gather(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        generator.addProvider(new LuaHelpProvider(generator, event.getExistingFileHelper()));
    }
}
