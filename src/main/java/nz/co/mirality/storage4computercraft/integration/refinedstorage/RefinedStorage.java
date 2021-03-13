package nz.co.mirality.storage4computercraft.integration.refinedstorage;

import com.refinedmods.refinedstorage.api.IRSAPI;
import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.network.node.INetworkNode;
import com.refinedmods.refinedstorage.api.util.IComparer;
import com.refinedmods.refinedstorage.api.util.IStackList;
import com.refinedmods.refinedstorage.api.util.StackListEntry;
import com.refinedmods.refinedstorage.apiimpl.API;
import com.refinedmods.refinedstorage.apiimpl.network.node.NetworkNode;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.common.crafting.StackList;
import net.minecraftforge.fluids.FluidStack;
import nz.co.mirality.storage4computercraft.integration.IStorageSearcher;
import nz.co.mirality.storage4computercraft.util.LuaConversion;
import nz.co.mirality.storage4computercraft.util.NBTUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class RefinedStorage implements IRefinedStorage {
    private final IRSAPI api;

    public RefinedStorage() {
        this.api = API.instance();

        LuaConversion.register(new RefinedStorageConverter());
    }

    @Override
    public Object getApi() {
        return this.api;
    }

    @Override
    public void init() {
        this.api.getNetworkNodeRegistry().add(RSPeripheralNetworkNode.ID,
                (tag, world, pos) -> readAndReturn(tag, new RSPeripheralNetworkNode(world, pos)));
    }

    @Override
    public Supplier<TileEntityType<?>> createPeripheral() {
        return () -> TileEntityType.Builder.of(RSPeripheralTile::new).build(null);
    }

    public static IStorageSearcher<ItemStack> storedItems(INetwork network) {
        return new StorageSearcher<>(() -> network.getItemStorageCache().getList());
    }

    public static IStorageSearcher<ItemStack> craftableItems(INetwork network) {
        return new StorageSearcher<>(() -> network.getItemStorageCache().getCraftablesList());
    }

    public static IStorageSearcher<FluidStack> storedFluids(INetwork network) {
        return new StorageSearcher<>(() -> network.getFluidStorageCache().getList());
    }

    public static IStorageSearcher<FluidStack> craftableFluids(INetwork network) {
        return new StorageSearcher<>(() -> network.getFluidStorageCache().getCraftablesList());
    }

    private static INetworkNode readAndReturn(CompoundNBT tag, NetworkNode node) {
        node.read(tag);
        return node;
    }

    private static class StorageSearcher<TStack> implements IStorageSearcher<TStack> {
        private final Supplier<IStackList<TStack>> sup;

        public StorageSearcher(final Supplier<IStackList<TStack>> sup) {
            this.sup = sup;
        }

        @Nonnull
        @Override
        public List<TStack> find() {
            Collection<StackListEntry<TStack>> entries = sup.get().getStacks();
            List<TStack> result = new ArrayList<>(entries.size());
            for (StackListEntry<TStack> entry : entries) {
                result.add(entry.getStack());
            }
            return result;
        }

        @Nullable
        @Override
        public TStack findFirst(@Nonnull TStack definition, boolean matchTags) {
            int flags = matchTags ? IComparer.COMPARE_NBT : 0;
            return sup.get().get(definition, flags);
        }

        @Nonnull
        @Override
        public List<TStack> findIgnoringTags(@Nonnull TStack definition) {
            Collection<StackListEntry<TStack>> entries = sup.get().getStacks(definition);
            List<TStack> result = new ArrayList<>(entries.size());
            for (StackListEntry<TStack> entry : entries) {
                result.add(entry.getStack());
            }
            return result;
        }

        @Nullable
        @Override
        public CompoundNBT findMatchingTagByHash(@Nonnull TStack definition, @Nonnull String nbtHash) {
            for (StackListEntry<TStack> search : sup.get().getStacks(definition)) {
                CompoundNBT tag = getTag(search.getStack());
                String hash = NBTUtil.toHash(tag);
                if (nbtHash.equals(hash)) {
                    return tag.copy();
                }
            }
            return null;
        }

        @Nullable
        private CompoundNBT getTag(@Nonnull TStack stack) {
            if (stack instanceof ItemStack) {
                return ((ItemStack) stack).getTag();
            }
            if (stack instanceof FluidStack) {
                return ((FluidStack) stack).getTag();
            }
            return null;
        }
    }
}
