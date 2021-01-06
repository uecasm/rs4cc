package nz.co.mirality.storage4computercraft;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RS4CC.ID)
public final class BackwardsCompatibility {
    public static final String OLD_MOD_ID = "refinedstorage4computercraft";
    private static final String OLD_PERIPHERAL = "peripheral";

    @SubscribeEvent
    public static void missingBlocks(RegistryEvent.MissingMappings<Block> e) {
        e.getAllMappings().stream()
                .filter(m -> m.key.getNamespace().equals(OLD_MOD_ID) && m.key.getPath().equals(OLD_PERIPHERAL))
                .forEach(m -> m.remap(RS4CCRegistry.RS_PERIPHERAL_BLOCK.get()));
    }

    @SubscribeEvent
    public static void missingItems(RegistryEvent.MissingMappings<Item> e) {
        e.getAllMappings().stream()
                .filter(m -> m.key.getNamespace().equals(OLD_MOD_ID) && m.key.getPath().equals(OLD_PERIPHERAL))
                .forEach(m -> m.remap(RS4CCRegistry.RS_PERIPHERAL_ITEM.get()));
    }
}
