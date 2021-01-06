package nz.co.mirality.storage4computercraft.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.config.PowerUnits;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import com.google.common.collect.ImmutableCollection;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import nz.co.mirality.storage4computercraft.RS4CC;
import nz.co.mirality.storage4computercraft.data.LuaDoc;
import nz.co.mirality.storage4computercraft.util.LuaConversion;
import nz.co.mirality.storage4computercraft.util.ServerWorker;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class MEPeripheral implements IPeripheral {
    public MEPeripheral(MEPeripheralGrid grid) {
        this.grid = grid;
    }

    private final MEPeripheralGrid grid;

    @Nonnull
    @Override
    public String getType() {
        return RS4CC.ME_PERIPHERAL_NAME;
    }

    @Override
    public void attach(@Nonnull IComputerAccess computer) {
        RS4CC.LOGGER.debug("Attached computer {} to network", computer.getID());

        ServerWorker.add(this.grid::computerConnected);
    }

    @Override
    public void detach(@Nonnull IComputerAccess computer) {
        RS4CC.LOGGER.debug("Detached computer {} from network", computer.getID());

        ServerWorker.add(this.grid::computerDisconnected);
    }

    @Nullable
    @Override
    public Object getTarget() {
        return this.grid.getTile();
    }

    @Override
    public boolean equals(@Nullable IPeripheral iPeripheral) {
        if (iPeripheral instanceof MEPeripheral) {
            return this.grid == ((MEPeripheral) iPeripheral).grid;
        }
        return false;
    }

    @Nonnull
    private static AppliedEnergistics getApi() {
        return (AppliedEnergistics) RS4CC.ME_API;
    }

    /**
     * Whether the network is connected.
     *
     * NOTE: Most other methods will return null if the network is not connected.
     *
     * @return true if the network is connected; false otherwise.
     * @since 1.2.0
     */
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 1, order = 1)
    public final boolean isConnected() {
        return this.grid.isOnline();
    }

    /**
     * Gets the energy usage of this network.
     * @return The RF/t energy usage, or null if disconnected.
     * @since 1.2.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 1, order = 4, returns = "number")
    public final Object[] getEnergyUsage() {
        IGrid grid = this.grid.getOnlineGrid();
        if (grid == null) {
            return disconnected();
        }
        final IEnergyGrid energy = grid.getCache(IEnergyGrid.class);
        return new Object[] { PowerUnits.AE.convertTo(PowerUnits.RF, energy.getAvgPowerUsage()) };
    }

    /**
     * Gets the total stored energy of this network.
     * @return The RF energy storage, or null if disconnected.
     * @since 1.2.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 1, order = 5, returns = "number")
    public final Object[] getEnergyStorage() {
        IGrid grid = this.grid.getOnlineGrid();
        if (grid == null) {
            return disconnected();
        }
        final IEnergyGrid energy = grid.getCache(IEnergyGrid.class);
        return new Object[] { PowerUnits.AE.convertTo(PowerUnits.RF, energy.getStoredPower()) };
    }

    /**
     * Gets all items and fluids either stored in or craftable by this system.
     * @return The list of items and fluids.
     * @since 1.2.0
     */
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 2, order = 1)
    public final Object[] getInventory() {
        IGrid grid = this.grid.getOnlineGrid();
        if (grid == null) {
            return disconnected();
        }

        final IStorageGrid storage = grid.getCache(IStorageGrid.class);
        final ICraftingGrid crafting = grid.getCache(ICraftingGrid.class);
        final IItemList<IAEItemStack> itemStorageList = storage.getInventory(getApi().getItemStorageChannel()).getStorageList();
        final IItemList<IAEFluidStack> fluidStorageList = storage.getInventory(getApi().getFluidStorageChannel()).getStorageList();
        final List<Object> result = new ArrayList<>(itemStorageList.size() + fluidStorageList.size());
        for (IAEItemStack stack : itemStorageList) {
            Map<Object, Object> details = AppliedEnergisticsConverter.convertStack(stack);
            details.put("crafting", crafting.requesting(stack));
            result.add(details);
        }
        for (IAEFluidStack stack : fluidStorageList) {
            Map<Object, Object> details = AppliedEnergisticsConverter.convertStack(stack);
            details.put("crafting", 0);     // AE2 doesn't support fluid crafting
            result.add(details);
        }
        return new Object[] { LuaConversion.convert(result) };
    }

    /**
     * Get all crafting patterns for one item in this network.
     * @param stack The item description, e.g. {name="minecraft:stone"}
     * @return A list of patterns for that item, or null if disconnected or there is no such pattern.
     * @throws LuaException The item description was invalid.
     * @since 1.2.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 5, order = 11, args = "table stack")
    public final Object[] getPatterns(final Map<?, ?> stack) throws LuaException {
        IGrid grid = this.grid.getOnlineGrid();
        if (grid == null) {
            return disconnected();
        }

        final IStorageGrid storage = grid.getCache(IStorageGrid.class);
        final ICraftingGrid crafting = grid.getCache(ICraftingGrid.class);

        ItemStack item = LuaConversion.getItemStack(stack, getApi().craftableItems(storage));
        IAEItemStack aeItem = getApi().getAEStack(item);
        ImmutableCollection<ICraftingPatternDetails> details = crafting.getCraftingFor(aeItem, null, 0, null);
        return new Object[] { LuaConversion.convert(details) };
    }

    /**
     * Gets a list of information about the crafting CPUs on this network.
     * @return A table of CPU info.
     * @since 1.2.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 5, order = 10)
    public final Object[] getCraftingCPUs() {
        IGrid grid = this.grid.getOnlineGrid();
        if (grid == null) {
            return disconnected();
        }

        final ICraftingGrid crafting = grid.getCache(ICraftingGrid.class);
        return new Object[] { LuaConversion.convert(crafting.getCpus()) };
    }

    /**
     * Schedules an item crafting task.
     * @param stack The item description, e.g. {name="minecraft:stone"}
     * @param count The number of items to be crafted (overrides count in stack, if specified)
     * @param canSchedule If false, does not actually start the craft (just reports if it's possible)
     * @param cpuName The name of the CPU to use to craft (optional)
     * @return A table describing the crafting task (even if not actually scheduled), or null if disconnected or unable to craft.
     * @throws LuaException The request was malformed.
     * @since 1.2.0
     */
    @SuppressWarnings("JavaDoc")
    @Nonnull
    @LuaFunction(mainThread = false)  // can't pullEvent on mainThread
    @LuaDoc(group = 5, order = 20, args = "table stack, [number count = stack.count = 1], [boolean canSchedule = true], [string preferredCpu = nil]", returns = "table")
    public final MethodResult scheduleTask(IComputerAccess computer, final Map<?, ?> stack, final Optional<Integer> count, final Optional<Boolean> canSchedule, final Optional<String> cpuName) {
        CraftingCallback callback = new CraftingCallback(computer, this.grid, stack, count, canSchedule.orElse(true), cpuName.orElse(null));
        ServerWorker.add(callback::start);
        return MethodResult.pullEvent(CraftingCallback.COMPLETE, callback);
    }

    // since we're not currently tracking crafting jobs, we can't cancel them.
    // for whatever reason the API doesn't provide a means to view and cancel crafting jobs
    // that you didn't start yourself.  (so the crafting status GUI is cheating.)

    /**
     * Extracts a fluid from the network and inserts it to a tank on the given side of the peripheral.
     *
     * NOTE: May extract a smaller amount of fluid than specified if the network is getting empty or the tank is getting full.
     *
     * @param args Arguments, consisting of:
     *               * stack: table -- the fluid to be extracted
     *               * [amount: number] -- the amount of fluid (overrides the value in stack if specified)
     *               * [direction: number/string] -- the direction of the tank from the peripheral
     *             Direction can either be a string "down"/"up"/"north"/"south"/"west"/"east" or the equivalent number (0 = down)
     * @return The amount of fluid inserted (or 0 if the network is empty or target is full); null if the network is disconnected or the tank is missing.
     * @throws LuaException The arguments are invalid.
     * @since 1.2.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 10, order = 2, args = "table stack, [number amount = stack.amount = 1000], [string/number direction = \"down\"]", returns = "number")
    public final Object[] extractFluid(final IArguments args) throws LuaException {
        IGrid grid = this.grid.getOnlineGrid();
        if (grid == null) {
            return disconnected();
        }

        final MEPeripheralTile tile = this.grid.getTile();
        final IStorageGrid storage = grid.getCache(IStorageGrid.class);
        final IMEMonitor<IAEFluidStack> inventory = storage.getInventory(getApi().getFluidStorageChannel());

        if (tile.getWorld() == null) { return disconnected(); }

        // First argument: the fluid stack to extract
        FluidStack stack = LuaConversion.getFluidStack(args.getTable(0), getApi().storedFluids(storage));
        IAEFluidStack aeStack = getApi().getAEStack(stack);
        if (aeStack == null) { return new Object[] { 0 }; }
        // Second argument: the amount of fluid to extract
        int amount = Math.max(0, args.optInt(1, stack.getAmount()));
        // Third argument: which direction to extract to
        Direction direction = getDirection(args, 2);

        // Get the tile-entity on the specified side
        TileEntity targetEntity = tile.getWorld().getTileEntity(tile.getPos().offset(direction));
        IFluidHandler handler = targetEntity != null ? targetEntity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, direction.getOpposite()).resolve().orElse(null) : null;
        if (handler == null) {
            return error("No tank on the given side");
        }

        // Simulate extracting the fluid and get the amount of fluid that can be extracted
        MachineSource source = new MachineSource(tile);
        aeStack.setStackSize(amount);
        IAEFluidStack extracted = inventory.extractItems(aeStack, Actionable.SIMULATE, source);
        long transferableAmount = extracted.getStackSize();

        if (transferableAmount <= 0) {
            return new Object[] { 0 };
        }

        // Simulate inserting the fluid and see how much we were able to insert
        int filled = handler.fill(AppliedEnergistics.getFluidStack(extracted), IFluidHandler.FluidAction.SIMULATE);

        // Abort early if we cannot insert fluid
        if (filled <= 0) {
            return new Object[] { 0 };
        }

        // Actually do it and return how much fluid we've inserted
        transferableAmount = filled;
        aeStack.setStackSize(transferableAmount);
        extracted = inventory.extractItems(aeStack, Actionable.MODULATE, source);
        filled = handler.fill(AppliedEnergistics.getFluidStack(extracted), IFluidHandler.FluidAction.EXECUTE);

        // If there's still some spillover, try to put it back
        if (filled < transferableAmount) {
            aeStack.setStackSize(transferableAmount - filled);
            inventory.injectItems(aeStack, Actionable.MODULATE, source);
            // and if that fails... well, you probably didn't care anyway.
        }

        return new Object[] { transferableAmount };
    }

    /**
     * Gets information about a fluid from the network.
     * @param stack The fluid description (e.g. {name="minecraft:lava"})
     * @param compareNBT If false, finds the first fluid of that type, ignoring tags
     * @param evenIfZero If true, reports default information about the fluid type even if not actually present in the network (this always ignores tags)
     * @return A table describing the fluid and how much of it is in the network,
     *         or empty if it is not in the network, or null if the network is disconnected.
     * @throws LuaException The fluid description was invalid.
     * @since 1.2.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 2, order = 12, args = "table stack, [boolean compareNBT = true], [boolean evenIfZero = false]")
    public Object[] getFluid(final Map<?, ?> stack, final Optional<Boolean> compareNBT, final Optional<Boolean> evenIfZero) throws LuaException {
        IGrid grid = this.grid.getOnlineGrid();
        if (grid == null) {
            return disconnected();
        }

        final IStorageGrid storage = grid.getCache(IStorageGrid.class);
        FluidStack fluid = LuaConversion.getFluidStack(stack, getApi().storedFluids(storage));
        fluid = getApi().storedFluids(storage).findFirst(fluid, compareNBT.orElse(true));

        if (fluid == null) {
            if (evenIfZero.orElse(false)) {
                fluid = LuaConversion.parseZeroFluidStack(stack);
            } else {
                fluid = FluidStack.EMPTY;
            }
        }
        return new Object[] { LuaConversion.convert(fluid) };
    }

    /**
     * Gets a list of all fluids currently in this network.
     * @param stack If specified, only returns fluids with the matching type (ignoring tags);
     *              if omitted, all fluid types.
     * @return A table array describing matching stored fluids, or null if the network is disconnected.
     * @since 1.2.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 2, order = 2, args = "[table stack]")
    public final Object[] getFluids(@Nonnull final Optional<Map<?, ?>> stack) throws LuaException {
        IGrid grid = this.grid.getOnlineGrid();
        if (grid == null) {
            return disconnected();
        }

        final IStorageGrid storage = grid.getCache(IStorageGrid.class);
        List<FluidStack> stacks = LuaConversion.getFluidStacks(stack.orElse(null), getApi().storedFluids(storage));
        return new Object[] { LuaConversion.convert(stacks) };
    }

    /**
     * Extracts an item from the network and inserts it to a container on the given side of the peripheral.
     *
     * NOTE: May extract a smaller number of items than specified if the network is getting empty or the container is getting full.
     *
     * @param args Arguments, consisting of:
     *               * stack: table -- the item to be extracted
     *               * [count: number] -- the number of items (overrides the value in stack if specified)
     *               * [direction: number/string] -- the direction of the container from the peripheral
     *             Direction can either be a string "down"/"up"/"north"/"south"/"west"/"east" or the equivalent number (0 = down)
     * @return The number of items inserted (or 0 if the network is empty or target is full); null if the network is disconnected or the container is missing.
     * @throws LuaException The arguments are invalid.
     * @since 1.2.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 10, order = 1, args = "table stack, [number count = stack.count = 1], [string/number direction = \"down\"]", returns = "number")
    public final Object[] extractItem(final IArguments args) throws LuaException {
        IGrid grid = this.grid.getOnlineGrid();
        if (grid == null) {
            return disconnected();
        }

        final MEPeripheralTile tile = this.grid.getTile();
        final IStorageGrid storage = grid.getCache(IStorageGrid.class);
        final IMEMonitor<IAEItemStack> inventory = storage.getInventory(getApi().getItemStorageChannel());

        if (tile.getWorld() == null) { return disconnected(); }

        // First argument: the item stack to extract
        ItemStack stack = LuaConversion.getItemStack(args.getTable(0), getApi().storedItems(storage));
        IAEItemStack aeStack = getApi().getAEStack(stack);
        if (aeStack == null) { return new Object[] { 0 }; }
        // Second argument: the number of items to extract
        int count = Math.max(0, args.optInt(1, stack.getCount()));
        // Third argument: which direction to extract to
        Direction direction = getDirection(args, 2);

        // Get the tile-entity on the specified side
        TileEntity targetEntity = tile.getWorld().getTileEntity(tile.getPos().offset(direction));
        IItemHandler handler = targetEntity != null ? targetEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite()).resolve().orElse(null) : null;
        if (handler == null) {
            return error("No item container on the given side");
        }

        // Simulate extracting the item and get the amount of items that can be extracted
        MachineSource source = new MachineSource(tile);
        aeStack.setStackSize(count);
        IAEItemStack extracted = inventory.extractItems(aeStack, Actionable.SIMULATE, source);
        long transferableAmount = extracted.getStackSize();

        if (transferableAmount <= 0) {
            return new Object[] { 0 };
        }

        // Simulate inserting the item and see how many we were able to insert
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, AppliedEnergistics.getItemStack(extracted), true);
        if (!remainder.isEmpty()) {
            transferableAmount -= remainder.getCount();
        }

        // Abort early if we cannot insert items
        if (transferableAmount <= 0) {
            return new Object[] { 0 };
        }

        // Actually do it and return how many items we've inserted
        aeStack.setStackSize(transferableAmount);
        extracted = inventory.extractItems(aeStack, Actionable.MODULATE, source);
        remainder = ItemHandlerHelper.insertItemStacked(handler, AppliedEnergistics.getItemStack(extracted), false);

        // If there's still some spillover, try to put it back
        if (!remainder.isEmpty()) {
            aeStack.setStackSize(remainder.getCount());
            inventory.injectItems(aeStack, Actionable.MODULATE, source);
            // and if that fails... well, you probably didn't care anyway.
        }

        return new Object[] { transferableAmount };
    }

    /**
     * Gets information about an tem from the network.
     * @param stack The item description (e.g. {name="minecraft:stone"})
     * @param compareNBT If false, finds the first item of that type, ignoring tags
     * @param evenIfZero If true, reports default information about the item type even if not actually present in the network (this always ignores tags)
     * @return A table describing the item and how much of it is in the network,
     *         or empty if it is not in the network, or null if the network is disconnected.
     * @throws LuaException The item description was invalid.
     * @since 1.2.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 2, order = 11, args = "table stack, [boolean compareNBT = true], [boolean evenIfZero = false]")
    public Object[] getItem(final Map<?, ?> stack, final Optional<Boolean> compareNBT, final Optional<Boolean> evenIfZero) throws LuaException {
        IGrid grid = this.grid.getOnlineGrid();
        if (grid == null) {
            return disconnected();
        }

        final IStorageGrid storage = grid.getCache(IStorageGrid.class);
        ItemStack item = LuaConversion.getItemStack(stack, getApi().storedItems(storage));
        item = getApi().storedItems(storage).findFirst(item, compareNBT.orElse(true));

        if (item == null) {
            if (evenIfZero.orElse(false)) {
                item = LuaConversion.parseZeroItemStack(stack);
            } else {
                item = ItemStack.EMPTY;
            }
        }
        return new Object[] { LuaConversion.convert(item) };
    }

    /**
     * Gets a list of all items currently in this network.
     * @param stack If specified, only returns items with the matching type (ignoring tags);
     *              if omitted, all item types.
     * @return A table array describing matching stored items, or null if the network is disconnected.
     * @since 1.2.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 2, order = 1, args = "[table stack]")
    public final Object[] getItems(@Nonnull final Optional<Map<?, ?>> stack) throws LuaException {
        IGrid grid = this.grid.getOnlineGrid();
        if (grid == null) {
            return disconnected();
        }

        final IStorageGrid storage = grid.getCache(IStorageGrid.class);
        List<ItemStack> stacks = LuaConversion.getItemStacks(stack.orElse(null), getApi().storedItems(storage));
        return new Object[] { LuaConversion.convert(stacks) };
    }

    // there doesn't appear to be any succinct way to read individual storages from
    // the network either (at least not without walking the whole grid network and
    // looking for specific interface implementations); so let's omit that for now.

    private static Object[] disconnected() {
        return error("not connected");
    }

    private static Object[] error(String message) {
        return new Object[] { null, message };
    }

    @SuppressWarnings("SameParameterValue")
    private static Direction getDirection(IArguments args, int index) throws LuaException {
        Object directionArg = args.get(index);
        if (directionArg instanceof String) {
            return args.getEnum(index, Direction.class);
        } else if (directionArg != null) {
            return Direction.byIndex(args.getInt(index));
        }
        return Direction.DOWN;
    }
}
