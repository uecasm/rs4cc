package nz.co.mirality.storage4computercraft.integration.refinedstorage;

import com.refinedmods.refinedstorage.api.autocrafting.task.ICalculationResult;
import com.refinedmods.refinedstorage.api.autocrafting.task.ICraftingTask;
import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.storage.IStorage;
import com.refinedmods.refinedstorage.api.storage.disk.IStorageDisk;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.api.util.IComparer;
import com.refinedmods.refinedstorage.api.util.IStackList;
import com.refinedmods.refinedstorage.apiimpl.API;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import nz.co.mirality.storage4computercraft.RS4CC;
import nz.co.mirality.storage4computercraft.data.LuaDoc;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import nz.co.mirality.storage4computercraft.util.LuaConversion;
import nz.co.mirality.storage4computercraft.util.ServerWorker;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class RSPeripheral implements IPeripheral {
    public RSPeripheral(RSPeripheralTile tile) {
        this.tile = tile;
    }

    private final RSPeripheralTile tile;

    @Nonnull
    private RSPeripheralNetworkNode getNode() {
        return this.tile.getNode();
    }

    @Nonnull
    @Override
    public String getType() {
        return RS4CC.RS_PERIPHERAL_NAME;
    }

    @Override
    public void attach(@Nonnull IComputerAccess computer) {
        RS4CC.LOGGER.debug("Attached computer {} to network", computer.getID());

        ServerWorker.add(() -> this.getNode().computerConnected());
    }

    @Override
    public void detach(@Nonnull IComputerAccess computer) {
        RS4CC.LOGGER.debug("Detached computer {} from network", computer.getID());

        ServerWorker.add(() -> this.getNode().computerDisconnected());
    }

    @Nullable
    @Override
    public Object getTarget() {
        return this.tile;
    }

    @Override
    public boolean equals(@Nullable IPeripheral iPeripheral) {
        if (iPeripheral instanceof RSPeripheral) {
            return this.tile == ((RSPeripheral) iPeripheral).tile;
        }
        return false;
    }

    @Nullable
    private INetwork getNetwork() {
        return getNode().getNetwork();
    }

    private static boolean isConnected(INetwork network) {
        return network != null && network.canRun();
    }

    /**
     * Whether the network is connected.
     *
     * NOTE: Most other methods will return null if the network is not connected.
     *
     * @return true if the network is connected; false otherwise.
     * @since 1.0.0
     */
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 1, order = 1)
    public final boolean isConnected() {
        return isConnected(this.getNetwork());
    }

    /**
     * Gets the energy usage of this network.
     * @return The RF/t energy usage, or null if disconnected.
     * @since 1.0.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 1, order = 4, returns = "number")
    public final Object[] getEnergyUsage() {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }
        return new Object[] { network.getEnergyUsage() };
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
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }
        return new Object[] { network.getEnergyStorage().getEnergyStored() };
    }

    /**
     * Gets the current crafting tasks of this network.
     * @return The list of crafting tasks, or null if disconnected.
     * @since 1.0.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 6, order = 1)
    public final Object[] getTasks() {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        Collection<ICraftingTask> tasks = network.getCraftingManager().getTasks();
        return new Object[] { LuaConversion.convert(tasks) };
    }

    /**
     * Get one item pattern of this network.
     * @param stack The item description, e.g. {name="minecraft:stone"}
     * @return The pattern for that item, or null if disconnected or there is no such pattern.
     * @throws LuaException The item description was invalid.
     * @since 1.0.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 5, order = 21, args = "table stack")
    public final Object[] getPattern(final Map<?, ?> stack) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        ItemStack items = LuaConversion.getItemStack(stack, RefinedStorage.craftableItems(network));
        return new Object[] { LuaConversion.convert(network.getCraftingManager().getPattern(items)) };
    }

    /**
     * Get one fluid pattern of this network.
     * @param stack The fluid description, e.g. {name="minecraft:lava"}
     * @return The pattern for that fluid, or null if disconnected or there is no such pattern.
     * @throws LuaException The fluid description was invalid.
     * @since 1.0.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 5, order = 22, args = "table stack")
    public final Object[] getFluidPattern(final Map<?, ?> stack) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        FluidStack fluid = LuaConversion.getFluidStack(stack, RefinedStorage.craftableFluids(network));
        return new Object[] { LuaConversion.convert(network.getCraftingManager().getPattern(fluid)) };
    }

    /**
     * Gets the item patterns of this network.
     * @param stack If specified, only returns patterns with the matching output item type (ignoring
     *              tags); if omitted, returns patterns for all item types.
     * @return An array of all the possible output items, or null if disconnected.
     * @since 1.0.0 for method, 1.1.0 for stack parameter
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 5, order = 1, args = "[table stack]")
    public final Object[] getPatterns(@Nonnull final Optional<Map<?, ?>> stack) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        List<ItemStack> outputs = LuaConversion.getItemStacks(stack.orElse(null), RefinedStorage.craftableItems(network));
        return new Object[] { LuaConversion.convert(outputs) };
    }

    /**
     * Gets the fluid patterns of this network.
     * @param stack If specified, only returns patterns with the matching output fluid type (ignoring
     *              tags); if omitted, returns patterns for all fluid types.
     * @return An array of all the possible output fluids, or null if disconnected.
     * @since 1.0.0 for method, 1.1.0 for stack parameter
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 5, order = 2, args = "[table stack]")
    public final Object[] getFluidPatterns(@Nonnull final Optional<Map<?, ?>> stack) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        List<FluidStack> outputs = LuaConversion.getFluidStacks(stack.orElse(null), RefinedStorage.craftableFluids(network));
        return new Object[] { LuaConversion.convert(outputs) };
    }

    /**
     * Whether a crafting pattern exists for this item.
     * @param stack The item description, e.g. {name="minecraft:stone"}
     * @return true if a pattern exists, false if it does not, or null if disconnected.
     * @throws LuaException The item description was invalid.
     * @since 1.0.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 5, order = 11, args = "table stack")
    public final Object[] hasPattern(final Map<?, ?> stack) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        ItemStack items = LuaConversion.getItemStack(stack, RefinedStorage.craftableItems(network));
        return new Object[] { network.getCraftingManager().getPattern(items) != null };
    }

    /**
     * Whether a crafting pattern exists for this fluid.
     * @param stack The fluid description, e.g. {name="minecraft:lava"}
     * @return true if a pattern exists, false if it does not, or null if disconnected.
     * @throws LuaException The fluid description was invalid.
     * @since 1.0.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 5, order = 12, args = "table stack")
    public final Object[] hasFluidPattern(final Map<?, ?> stack) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        FluidStack fluid = LuaConversion.getFluidStack(stack, RefinedStorage.craftableFluids(network));
        return new Object[] { network.getCraftingManager().getPattern(fluid) != null };
    }

    /**
     * Schedules an item crafting task.
     * @param stack The item description, e.g. {name="minecraft:stone"}
     * @param count The number of items to be crafted (overrides count in stack, if specified)
     * @param canSchedule If false, does not actually start the craft (just reports if it's possible)
     * @return A table describing the crafting task (even if not actually scheduled), or null if disconnected or unable to craft.
     * @throws LuaException The request was malformed.
     * @since 1.0.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 6, order = 2, args = "table stack, [number count = 1], [boolean canSchedule = true]")
    public final Object[] scheduleTask(final Map<?, ?> stack, final Optional<Integer> count, final Optional<Boolean> canSchedule) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        ItemStack output = LuaConversion.getItemStack(stack, RefinedStorage.craftableItems(network));
        int quantity = Math.max(1, count.orElse(output.getCount()));

        ICalculationResult result = network.getCraftingManager().create(output, quantity);
        if (result.isOk()) {
            ICraftingTask task = result.getTask();
            if (canSchedule.orElse(true)) {
                if (task == null) return error("Crafting task unexpectedly null when OK");
                network.getCraftingManager().start(task);
            }
            return new Object[] { LuaConversion.convert(task) };
        }

        switch (result.getType()) {
            case NO_PATTERN: return error("No pattern found");
            case MISSING: return error("Inputs missing");
            default: return error("Crafting error");
        }
    }

    /**
     * Schedules a fluid crafting task.
     * @param stack The fluid description, e.g. {name="minecraft:lava"}
     * @param amount The amount of fluid to be crafted (mB); overrides amount in stack if specified
     * @param canSchedule If false, does not actually start the craft (just reports if it's possible)
     * @return A table describing the crafting task (even if not actually scheduled), or null if disconnected or unable to craft.
     * @throws LuaException The request was malformed.
     * @since 1.0.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 6, order = 3, args = "table stack, [number amount = 1000], [boolean canSchedule = true]")
    public final Object[] scheduleFluidTask(final Map<?, ?> stack, final Optional<Integer> amount, final Optional<Boolean> canSchedule) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        FluidStack output = LuaConversion.getFluidStack(stack, RefinedStorage.craftableFluids(network));
        int quantity = Math.max(1, amount.orElse(output.getAmount()));

        ICalculationResult result = network.getCraftingManager().create(output, quantity);
        if (result.isOk()) {
            ICraftingTask task = result.getTask();
            if (canSchedule.orElse(true)) {
                if (task == null) return error("Crafting task unexpectedly null when OK");
                network.getCraftingManager().start(task);
            }
            return new Object[] { LuaConversion.convert(task) };
        }

        switch (result.getType()) {
            case NO_PATTERN: return error("No pattern found");
            case MISSING: return error("Inputs missing");
            default: return error("Crafting error");
        }
    }

    /**
     * Cancels an item crafting task.
     * @param stack The item description, e.g. {name="minecraft:stone"}
     * @return The number of crafting tasks cancelled, or null if disconnected.
     * @throws LuaException The item description was invalid.
     * @since 1.0.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 6, order = 11, args = "table stack")
    public final Object[] cancelTask(final Map<?, ?> stack) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        ItemStack items = LuaConversion.getItemStack(stack, RefinedStorage.craftableItems(network));

        int count = 0;
        for (ICraftingTask task : network.getCraftingManager().getTasks()) {
            if (task.getRequested().getItem() != null) {
                if (API.instance().getComparer().isEqual(task.getRequested().getItem(), items, IComparer.COMPARE_NBT)) {
                    network.getCraftingManager().cancel(task.getId());
                    ++count;
                }
            }
        }
        return new Object[] { count };
    }

    /**
     * Cancels a fluid crafting task.
     * @param stack The fluid description, e.g. {name="minecraft:lava"}
     * @return The number of crafting tasks cancelled, or null if disconnected.
     * @throws LuaException The fluid description was invalid.
     * @since 1.0.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 6, order = 12, args = "table stack")
    public final Object[] cancelFluidTask(final Map<?, ?> stack) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        FluidStack fluid = LuaConversion.getFluidStack(stack, RefinedStorage.craftableFluids(network));

        int count = 0;
        for (ICraftingTask task : network.getCraftingManager().getTasks()) {
            if (task.getRequested().getFluid() != null) {
                if (API.instance().getComparer().isEqual(task.getRequested().getFluid(), fluid, IComparer.COMPARE_NBT)) {
                    network.getCraftingManager().cancel(task.getId());
                    ++count;
                }
            }
        }
        return new Object[] { count };
    }

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
     * @since 1.0.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 10, order = 2, args = "table stack, [number amount = stack.amount = 1000], [string/number direction = \"down\"]", returns = "number")
    public final Object[] extractFluid(final IArguments args) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        // First argument: fluid and amount.
        FluidStack stack = LuaConversion.getFluidStack(args.getTable(0), RefinedStorage.storedFluids(network));
        // Second argument: amount of fluid
        args.optInt(1).ifPresent(stack::setAmount);
        // Third argument: which direction to extract to
        Direction direction = getDirection(args, 2);

        // Get the tile-entity on the specified side
        TileEntity targetEntity = network.getWorld().getTileEntity(this.tile.getPos().offset(direction));
        IFluidHandler handler = targetEntity != null ? targetEntity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, direction.getOpposite()).resolve().orElse(null) : null;
        if (handler == null) {
            return error("No fluid tank on the given side");
        }

        FluidStack extractedSim = network.extractFluid(stack, stack.getAmount(), Action.SIMULATE);
        if (extractedSim.isEmpty()) {
            return new Object[] { 0 };
        }

        // Simulate inserting the fluid and see how much we were able to insert
        int filledAmountSim = handler.fill(extractedSim, IFluidHandler.FluidAction.SIMULATE);
        if (filledAmountSim <= 0) {
            return new Object[] { 0 };
        }

        // Actually do it and return how much fluid we've inserted
        FluidStack extractedActual = network.extractFluid(stack, filledAmountSim, Action.PERFORM);
        int filledAmountActual = handler.fill(extractedActual, IFluidHandler.FluidAction.EXECUTE);

        // Attempt to insert excess fluid back into the network
        // This shouldn't need to happen for most tanks, unless input cap decreases based on insert amount
        if (extractedActual.getAmount() > filledAmountActual) {
            network.insertFluid(stack, extractedActual.getAmount() - filledAmountActual, Action.PERFORM);
        }

        return new Object[] { filledAmountActual };
    }

    /**
     * Gets information about a fluid from the network.
     * @param stack The fluid description (e.g. {name="minecraft:lava"})
     * @param compareNBT If false, finds the first fluid of that type, ignoring tags
     * @param evenIfZero If true, reports default information about the fluid type even if not actually present in the network (this always ignores tags)
     * @return A table describing the fluid and how much of it is in the network,
     *         or empty if it is not in the network, or null if the network is disconnected.
     * @throws LuaException The fluid description was invalid.
     * @since 1.0.0; 1.1.0 adds evenIfZero
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 2, order = 12, args = "table stack, [boolean compareNBT = true], [boolean evenIfZero = false]")
    public Object[] getFluid(final Map<?, ?> stack, final Optional<Boolean> compareNBT, final Optional<Boolean> evenIfZero) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        FluidStack fluid = LuaConversion.getFluidStack(stack, RefinedStorage.storedFluids(network));
        fluid = RefinedStorage.storedFluids(network).findFirst(fluid, compareNBT.orElse(true));
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
     * @since 1.0.0 for method, 1.1.0 for stack parameter
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 2, order = 2, args = "[table stack]")
    public final Object[] getFluids(@Nonnull final Optional<Map<?, ?>> stack) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        List<FluidStack> stacks = LuaConversion.getFluidStacks(stack.orElse(null), RefinedStorage.storedFluids(network));
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
     * @since 1.0.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 10, order = 1, args = "table stack, [number count = stack.count = 1], [string/number direction = \"down\"]", returns = "number")
    public final Object[] extractItem(final IArguments args) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        // First argument: the item stack to extract
        ItemStack stack = LuaConversion.getItemStack(args.getTable(0), RefinedStorage.storedItems(network));
        // Second argument: the number of items to extract
        int count = Math.max(0, args.optInt(1, stack.getCount()));
        // Third argument: which direction to extract to
        Direction direction = getDirection(args, 2);

        // Get the tile-entity on the specified side
        TileEntity targetEntity = network.getWorld().getTileEntity(this.tile.getPos().offset(direction));
        IItemHandler handler = targetEntity != null ? targetEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite()).resolve().orElse(null) : null;
        if (handler == null) {
            return error("No item container on the given side");
        }

        // Simulate extracting the item and get the amount of items that can be extracted
        ItemStack extracted = network.extractItem(stack, count, Action.SIMULATE);
        if (extracted.isEmpty()) {
            return new Object[] { 0 };
        }

        int transferableAmount = extracted.getCount();

        // Simulate inserting the item and see how many we were able to insert
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, extracted, true);
        if (!remainder.isEmpty()) {
            transferableAmount -= remainder.getCount();
        }

        // Abort early if we can not insert items
        if (transferableAmount <= 0) {
            return new Object[] { 0 };
        }

        // Actually do it and return how many items we've inserted
        extracted = network.extractItem(stack, transferableAmount, Action.PERFORM);
        remainder = ItemHandlerHelper.insertItemStacked(handler, extracted, false);

        if (!remainder.isEmpty()) {
            network.insertItem(remainder, remainder.getCount(), Action.PERFORM);
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
     * @since 1.0.0; 1.1.0 adds evenIfZero
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 2, order = 11, args = "table stack, [boolean compareNBT = true], [boolean evenIfZero = false]")
    public Object[] getItem(final Map<?, ?> stack, final Optional<Boolean> compareNBT, final Optional<Boolean> evenIfZero) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        ItemStack item = LuaConversion.getItemStack(stack, RefinedStorage.storedItems(network));
        item = RefinedStorage.storedItems(network).findFirst(item, compareNBT.orElse(true));
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
     * @since 1.0.0 for method, 1.1.0 for stack parameter
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 2, order = 1, args = "[table stack]")
    public final Object[] getItems(@Nonnull final Optional<Map<?, ?>> stack) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        List<ItemStack> stacks = LuaConversion.getItemStacks(stack.orElse(null), RefinedStorage.storedItems(network));
        return new Object[] { LuaConversion.convert(stacks) };
    }

    /**
     * Gets a list of all connected storage disks and blocks in this network.
     * @return A table describing the connected storage, or null if the network is disconnected.
     * @since 1.0.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    @LuaDoc(group = 1, order = 20)
    public final Object[] getStorages() {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        int totalItemStored = 0;
        int totalItemCapacity = 0;

        int totalFluidStored = 0;
        int totalFluidCapacity = 0;

        List<HashMap<String, Object>> devices = new ArrayList<>();

        for (IStorage<ItemStack> s : network.getItemStorageCache().getStorages()) {
            if (s instanceof IStorageDisk) {
                IStorageDisk<ItemStack> disk = (IStorageDisk<ItemStack>) s;

                HashMap<String, Object> data = new HashMap<>();

                data.put("type", "item");
                data.put("usage", disk.getStored());
                data.put("capacity", disk.getCapacity());

                totalItemStored += disk.getStored();
                totalItemCapacity += disk.getCapacity();

                devices.add(data);
            }
        }

        for (IStorage<FluidStack> s : network.getFluidStorageCache().getStorages()) {
            if (s instanceof IStorageDisk) {
                IStorageDisk<FluidStack> disk = (IStorageDisk<FluidStack>) s;

                HashMap<String, Object> data = new HashMap<>();

                data.put("type", "fluid");
                data.put("usage", disk.getStored());
                data.put("capacity", disk.getCapacity());

                totalFluidStored += disk.getStored();
                totalFluidCapacity += disk.getCapacity();

                devices.add(data);
            }
        }

        HashMap<String, Integer> itemTotals = new HashMap<>();
        itemTotals.put("usage", totalItemStored);
        itemTotals.put("capacity", totalItemCapacity);

        HashMap<String, Integer> fluidTotals = new HashMap<>();
        fluidTotals.put("usage", totalFluidStored);
        fluidTotals.put("capacity", totalFluidCapacity);

        HashMap<String, Object> totals = new HashMap<>();
        totals.put("item", itemTotals);
        totals.put("fluid", fluidTotals);

        HashMap<String, Object> response = new HashMap<>();
        response.put("total", totals);
        response.put("devices", devices);

        return new Object[] { response };
    }

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
