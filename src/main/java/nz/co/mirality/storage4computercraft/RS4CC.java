package nz.co.mirality.storage4computercraft;

import net.minecraft.item.ItemGroup;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import nz.co.mirality.storage4computercraft.data.ModLoadedLootCondition;
import nz.co.mirality.storage4computercraft.integration.ae2.IAppliedEnergistics;
import nz.co.mirality.storage4computercraft.integration.refinedstorage.IRefinedStorage;
import nz.co.mirality.storage4computercraft.util.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

@Mod(RS4CC.ID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RS4CC {
    public static final String ID = "storage4computercraft";
    public static final String NAME = "Storage for ComputerCraft";

    public static final String TOP_MOD_ID = "theoneprobe";
    public static final String CC_MOD_ID = "computercraft";
    public static final String RS_MOD_ID = "refinedstorage";
    public static final String AE2_MOD_ID = "appliedenergistics2";

    public static final String RS_PERIPHERAL_ID = "rs_peripheral";
    public static final String RS_PERIPHERAL_NAME = RS_MOD_ID;

    public static final String ME_PERIPHERAL_ID = "me_peripheral";
    public static final String ME_PERIPHERAL_NAME = "ae2";

    public static final RS4CCConfig CONFIG = new RS4CCConfig();
    public static final Logger LOGGER = LogManager.getLogger();

    public static final Supplier<ItemGroup> GROUP = Lazy.of(RS4CC::getComputerCraftGroup);

    public static IRefinedStorage RS_API;
    public static IAppliedEnergistics ME_API;

    public RS4CC() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, CONFIG.getSpec());
        RS4CCRegistry.init();
    }

    @SubscribeEvent
    public static void preInit(FMLConstructModEvent e) {
        if (ModList.get().isLoaded(BackwardsCompatibility.OLD_MOD_ID)) {
            throw new IllegalStateException("Storage4ComputerCraft supersedes RefinedStorage4ComputerCraft; delete the latter");
        }

        RS_API = (IRefinedStorage) Platform.maybeLoadIntegration(RS_MOD_ID, "refinedstorage.RefinedStorage").orElse(null);
        ME_API = (IAppliedEnergistics) Platform.maybeLoadIntegration(AE2_MOD_ID, "ae2.AppliedEnergistics").orElse(null);
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent e) {
        Registry.register(Registry.LOOT_CONDITION_TYPE, new ResourceLocation(ID, "mod_loaded"),
                ModLoadedLootCondition.TYPE);

        RS4CCRegistry.RS_PERIPHERAL_ITEM.get().setEnabled(RS_API != null);
        RS4CCRegistry.ME_PERIPHERAL_ITEM.get().setEnabled(ME_API != null);

        if (RS_API != null) RS_API.init();
        if (ME_API != null) ME_API.init();
    }

    private static ItemGroup getComputerCraftGroup() {
        // note that the AE2 group is constructed in a way that items cannot be
        // arbitrarily added to it... so let's just add both peripherals to the
        // CC group instead.  (definitely no point in making a new group just for this)

        for (ItemGroup group : ItemGroup.GROUPS) {
            if (group.getPath().equals(CC_MOD_ID)) return group;
        }

        // couldn't find it for some reason...
        return ItemGroup.MISC;
    }
}
