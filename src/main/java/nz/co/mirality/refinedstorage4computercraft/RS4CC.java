package nz.co.mirality.refinedstorage4computercraft;

import com.refinedmods.refinedstorage.RS;
import com.refinedmods.refinedstorage.api.network.node.INetworkNode;
import com.refinedmods.refinedstorage.apiimpl.API;
import com.refinedmods.refinedstorage.apiimpl.network.node.NetworkNode;
import com.refinedmods.refinedstorage.item.blockitem.BaseBlockItem;
import dan200.computercraft.api.ComputerCraftAPI;
import nz.co.mirality.refinedstorage4computercraft.blocks.PeripheralBlock;
import nz.co.mirality.refinedstorage4computercraft.nodes.PeripheralNetworkNode;
import nz.co.mirality.refinedstorage4computercraft.tiles.PeripheralTile;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(RS4CC.ID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RS4CC {
    public static final String ID = "refinedstorage4computercraft";
    public static final String NAME = "RefinedStorage for ComputerCraft Integration";

    public static final String PERIPHERAL_ID = "peripheral";
    public static final String PERIPHERAL_NAME = "refinedstorage";

    public static final RS4CCConfig CONFIG = new RS4CCConfig();
    public static final Logger LOGGER = LogManager.getLogger();

    public RS4CC() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, CONFIG.getSpec());
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent e) {
        API.instance().getNetworkNodeRegistry().add(PeripheralNetworkNode.ID,
                (tag, world, pos) -> readAndReturn(tag, new PeripheralNetworkNode(world, pos)));
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> e) {
        e.getRegistry().registerAll(new PeripheralBlock().setRegistryName(PERIPHERAL_ID));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> e) {
        e.getRegistry().registerAll(new BaseBlockItem(RS4CCBlocks.PERIPHERAL,
                new Item.Properties().group(RS.MAIN_GROUP)).setRegistryName(PERIPHERAL_ID));
    }

    @SubscribeEvent
    public static void registerTiles(RegistryEvent.Register<TileEntityType<?>> e) {
        e.getRegistry().registerAll(TileEntityType.Builder.create(PeripheralTile::new)
                .build(null).setRegistryName(PERIPHERAL_ID));
    }

    private static INetworkNode readAndReturn(CompoundNBT tag, NetworkNode node) {
        node.read(tag);
        return node;
    }
}
