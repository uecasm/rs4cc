package nz.co.mirality.storage4computercraft;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import nz.co.mirality.storage4computercraft.blocks.MEPeripheralBlock;
import nz.co.mirality.storage4computercraft.blocks.RSPeripheralBlock;
import nz.co.mirality.storage4computercraft.integration.IStorageSystem;
import nz.co.mirality.storage4computercraft.integration.ae2.IAppliedEnergistics;
import nz.co.mirality.storage4computercraft.integration.refinedstorage.IRefinedStorage;
import nz.co.mirality.storage4computercraft.items.BaseBlockItem;
import nz.co.mirality.storage4computercraft.tiles.MEFakeTile;
import nz.co.mirality.storage4computercraft.tiles.RSFakeTile;

import javax.annotation.Nonnull;
import java.util.function.Function;
import java.util.function.Supplier;

public final class RS4CCRegistry {
    private static final DeferredRegister<Block> BLOCKS
            = DeferredRegister.create(ForgeRegistries.BLOCKS, RS4CC.ID);
    private static final DeferredRegister<Item> ITEMS
            = DeferredRegister.create(ForgeRegistries.ITEMS, RS4CC.ID);
    private static final DeferredRegister<TileEntityType<?>> TILES
            = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, RS4CC.ID);

    public static void init() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        BLOCKS.register(bus);
        ITEMS.register(bus);
        TILES.register(bus);
    }

    public static final RegistryObject<RSPeripheralBlock> RS_PERIPHERAL_BLOCK
            = BLOCKS.register(RS4CC.RS_PERIPHERAL_ID, RSPeripheralBlock::new);
    public static final RegistryObject<BaseBlockItem> RS_PERIPHERAL_ITEM
            = ITEMS.register(RS4CC.RS_PERIPHERAL_ID,
                () -> new BaseBlockItem(RS_PERIPHERAL_BLOCK.get(), new Item.Properties().tab(RS4CC.GROUP.get())));
    public static final RegistryObject<TileEntityType<?>> RS_PERIPHERAL_TILE
            = TILES.register(RS4CC.RS_PERIPHERAL_ID, fakeable(() -> RS4CC.RS_API, IRefinedStorage::createPeripheral, RSFakeTile::new));

    public static final RegistryObject<MEPeripheralBlock> ME_PERIPHERAL_BLOCK
            = BLOCKS.register(RS4CC.ME_PERIPHERAL_ID, MEPeripheralBlock::new);
    public static final RegistryObject<BaseBlockItem> ME_PERIPHERAL_ITEM
            = ITEMS.register(RS4CC.ME_PERIPHERAL_ID,
            () -> new BaseBlockItem(ME_PERIPHERAL_BLOCK.get(), new Item.Properties().tab(RS4CC.GROUP.get())));
    public static final RegistryObject<TileEntityType<?>> ME_PERIPHERAL_TILE
            = TILES.register(RS4CC.ME_PERIPHERAL_ID, fakeable(() -> RS4CC.ME_API, IAppliedEnergistics::createPeripheral, MEFakeTile::new));

    @Nonnull
    private static <A extends IStorageSystem> Supplier<TileEntityType<?>> fakeable(@Nonnull Supplier<A> storage, @Nonnull Function<A, Supplier<TileEntityType<?>>> real, @Nonnull Supplier<? extends TileEntity> fake) {
        return () -> storage.get() != null
                ? real.apply(storage.get()).get()
                : TileEntityType.Builder.of(fake).build(null);
    }

    private RS4CCRegistry() {}
}
