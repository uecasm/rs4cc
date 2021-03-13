package nz.co.mirality.storage4computercraft.integration.ae2;

import appeng.api.AEAddon;
import appeng.api.IAEAddon;
import appeng.api.IAppEngApi;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fluids.FluidStack;
import nz.co.mirality.storage4computercraft.RS4CC;
import nz.co.mirality.storage4computercraft.integration.IStorageSearcher;
import nz.co.mirality.storage4computercraft.util.LuaConversion;
import nz.co.mirality.storage4computercraft.util.NBTUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@AEAddon
public class AppliedEnergistics implements IAppliedEnergistics, IAEAddon {
    private static IAppEngApi api;

    @Override
    public void onAPIAvailable(IAppEngApi api) {
        AppliedEnergistics.api = api;
        // this needs to be an instance method that sets a static field,
        // for reasons best known to the AE2 authors...

        LuaConversion.register(new AppliedEnergisticsConverter());
    }

    @Override
    public IAppEngApi getApi() { return AppliedEnergistics.api; }

    @Override
    public void init() {
    }

    @Override
    public Supplier<TileEntityType<?>> createPeripheral() {
        return () -> TileEntityType.Builder.of(MEPeripheralTile::new).build(null);
    }

    @Nonnull
    public <T extends IAEStack<T>, C extends IStorageChannel<T>> C getStorageChannel(Class<C> type) {
        return getApi().storage().getStorageChannel(type);
    }

    @Nonnull
    public IItemStorageChannel getItemStorageChannel() {
        return getStorageChannel(IItemStorageChannel.class);
    }

    @Nonnull
    public IFluidStorageChannel getFluidStorageChannel() {
        return getStorageChannel(IFluidStorageChannel.class);
    }

    @Nullable
    public IAEItemStack getAEStack(ItemStack stack) {
        return getItemStorageChannel().createStack(stack);
    }

    @Nullable
    public IAEFluidStack getAEStack(FluidStack stack) {
        return getFluidStorageChannel().createStack(stack);
    }

    @Nonnull
    public static ItemStack getItemStack(IAEItemStack stack) {
        // this works better than stack.createItemStack() for craft-only zero stacks
        ItemStack itemStack = stack.getDefinition().copy();
        itemStack.setCount((int) Math.min(2147483647L, stack.getStackSize()));
        return itemStack;
    }

    @Nonnull
    public static FluidStack getFluidStack(IAEFluidStack stack) { return stack.getFluidStack(); }

    public IStorageSearcher<ItemStack> storedItems(IStorageGrid storage) {
        return new ItemStorageSearcher(() -> storage.getInventory(getItemStorageChannel()).getStorageList(), true, false);
    }

    public IStorageSearcher<ItemStack> craftableItems(IStorageGrid storage) {
        return new ItemStorageSearcher(() -> storage.getInventory(getItemStorageChannel()).getStorageList(), false, true);
    }

    public IStorageSearcher<FluidStack> storedFluids(IStorageGrid storage) {
        return new FluidStorageSearcher(() -> storage.getInventory(getFluidStorageChannel()).getStorageList());
    }

    private static class ItemStorageSearcher implements IStorageSearcher<ItemStack> {
        @Nonnull
        private final Supplier<IItemList<IAEItemStack>> sup;
        private final boolean includeStored;
        private final boolean includeCraftable;

        public ItemStorageSearcher(@Nonnull final Supplier<IItemList<IAEItemStack>> sup,
                                   boolean includeStored, boolean includeCraftable) {
            this.sup = sup;
            this.includeStored = includeStored;
            this.includeCraftable = includeCraftable;
        }

        private boolean isInteresting(@Nonnull IAEItemStack stack) {
            if (this.includeStored && stack.getStackSize() > 0) return true;
            if (this.includeCraftable && stack.isCraftable()) return true;
            return false;
        }

        @Nonnull
        @Override
        public List<ItemStack> find() {
            IItemList<IAEItemStack> list = this.sup.get();
            List<ItemStack> result = new ArrayList<>(list.size());
            for (IAEItemStack aeStack : list) {
                if (isInteresting(aeStack)) {
                    result.add(getItemStack(aeStack));
                }
            }
            return result;
        }

        @Nullable
        @Override
        public ItemStack findFirst(@Nonnull ItemStack definition, boolean matchTags) {
            IItemList<IAEItemStack> list = this.sup.get();
            for (IAEItemStack aeStack : list) {
                if (isInteresting(aeStack) && aeStack.getItem().equals(definition.getItem())) {
                    if (!matchTags || ItemStack.tagMatches(definition, aeStack.getDefinition())) {
                        return getItemStack(aeStack);
                    }
                }
            }
            return null;
        }

        @Nonnull
        @Override
        public List<ItemStack> findIgnoringTags(@Nonnull ItemStack definition) {
            IItemList<IAEItemStack> list = this.sup.get();
            List<ItemStack> result = new ArrayList<>();
            for (IAEItemStack aeStack : list) {
                if (isInteresting(aeStack) && aeStack.getItem().equals(definition.getItem())) {
                    result.add(getItemStack(aeStack));
                }
            }
            return result;
        }

        @Nullable
        @Override
        public CompoundNBT findMatchingTagByHash(@Nonnull ItemStack definition, @Nonnull String nbtHash) {
            IItemList<IAEItemStack> list = this.sup.get();
            for (IAEItemStack aeStack : list) {
                if (isInteresting(aeStack) && aeStack.getItem().equals(definition.getItem())) {
                    CompoundNBT tag = getItemStack(aeStack).getTag();
                    String hash = NBTUtil.toHash(tag);
                    if (nbtHash.equals(hash)) {
                        return tag.copy();
                    }
                }
            }
            return null;
        }
    }

    private static class FluidStorageSearcher implements IStorageSearcher<FluidStack> {
        @Nonnull
        private final Supplier<IItemList<IAEFluidStack>> sup;

        public FluidStorageSearcher(@Nonnull final Supplier<IItemList<IAEFluidStack>> sup) {
            this.sup = sup;
        }

        private boolean isInteresting(@Nonnull IAEFluidStack stack) {
            if (stack.getStackSize() > 0) return true;
            return false;
        }

        @Nonnull
        @Override
        public List<FluidStack> find() {
            IItemList<IAEFluidStack> list = this.sup.get();
            List<FluidStack> result = new ArrayList<>(list.size());
            for (IAEFluidStack aeStack : list) {
                if (isInteresting(aeStack)) {
                    result.add(getFluidStack(aeStack));
                }
            }
            return result;
        }

        @Nullable
        @Override
        public FluidStack findFirst(@Nonnull FluidStack definition, boolean matchTags) {
            IItemList<IAEFluidStack> list = this.sup.get();
            for (IAEFluidStack aeStack : list) {
                if (isInteresting(aeStack) && aeStack.getFluid().isSame(definition.getFluid())) {
                    if (!matchTags || FluidStack.areFluidStackTagsEqual(definition, aeStack.getFluidStack())) {
                        return getFluidStack(aeStack);
                    }
                }
            }
            return null;
        }

        @Nonnull
        @Override
        public List<FluidStack> findIgnoringTags(@Nonnull FluidStack definition) {
            IItemList<IAEFluidStack> list = this.sup.get();
            List<FluidStack> result = new ArrayList<>();
            for (IAEFluidStack aeStack : list) {
                if (isInteresting(aeStack) && aeStack.getFluid().isSame(definition.getFluid())) {
                    result.add(getFluidStack(aeStack));
                }
            }
            return result;
        }

        @Nullable
        @Override
        public CompoundNBT findMatchingTagByHash(@Nonnull FluidStack definition, @Nonnull String nbtHash) {
            IItemList<IAEFluidStack> list = this.sup.get();
            for (IAEFluidStack aeStack : list) {
                if (isInteresting(aeStack) && aeStack.getFluid().isSame(definition.getFluid())) {
                    CompoundNBT tag = getFluidStack(aeStack).getTag();
                    String hash = NBTUtil.toHash(tag);
                    if (nbtHash.equals(hash)) {
                        return tag.copy();
                    }
                }
            }
            return null;
        }
    }
}
