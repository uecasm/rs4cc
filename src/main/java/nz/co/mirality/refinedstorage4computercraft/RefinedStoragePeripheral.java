package nz.co.mirality.refinedstorage4computercraft;

import com.refinedmods.refinedstorage.api.autocrafting.ICraftingPattern;
import com.refinedmods.refinedstorage.api.autocrafting.task.ICalculationResult;
import com.refinedmods.refinedstorage.api.autocrafting.task.ICraftingTask;
import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.storage.IStorage;
import com.refinedmods.refinedstorage.api.storage.disk.IStorageDisk;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.api.util.IComparer;
import com.refinedmods.refinedstorage.apiimpl.API;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import nz.co.mirality.refinedstorage4computercraft.nodes.PeripheralNetworkNode;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class RefinedStoragePeripheral implements IPeripheral {
    public RefinedStoragePeripheral(PeripheralNetworkNode node) {
        this.node = node;
    }

    private final PeripheralNetworkNode node;

    @Nonnull
    @Override
    public String getType() {
        return this.node.getId().toString();
    }

    @Override
    public void attach(@Nonnull IComputerAccess computer) {
        RS4CC.LOGGER.debug("Attached computer {} to network", computer.getID());
        this.node.computerConnected();
    }

    @Override
    public void detach(@Nonnull IComputerAccess computer) {
        RS4CC.LOGGER.debug("Detached computer {} from network", computer.getID());
        this.node.computerDisconnected();
    }

    @Nullable
    @Override
    public Object getTarget() {
        return this.node;
    }

    @Override
    public boolean equals(@Nullable IPeripheral iPeripheral) {
        if (iPeripheral instanceof RefinedStoragePeripheral) {
            return this.node == ((RefinedStoragePeripheral) iPeripheral).node;
        }
        return false;
    }

    @Nullable
    private INetwork getNetwork() {
        return this.node.getNetwork();
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
    public final Object[] getEnergyUsage() {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }
        return new Object[] { network.getEnergyUsage() };
    }

    /**
     * Gets the current crafting tasks of this network.
     * @return The list of crafting tasks, or null if disconnected.
     * @since 1.0.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
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
    public final Object[] getPattern(final Map<?, ?> stack) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        ItemStack items = LuaConversion.getItemStack(stack);
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
    public final Object[] getFluidPattern(final Map<?, ?> stack) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        FluidStack fluid = LuaConversion.getFluidStack(stack, FluidAttributes.BUCKET_VOLUME);
        return new Object[] { LuaConversion.convert(network.getCraftingManager().getPattern(fluid)) };
    }

    /**
     * Gets the item patterns of this network.
     * @return An array of all the possible output items, or null if disconnected.
     * @since 1.0.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    public final Object[] getPatterns() {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        List<ItemStack> patterns = new LinkedList<>();
        for (ICraftingPattern pattern : network.getCraftingManager().getPatterns()) {
            if (!pattern.getOutputs().isEmpty()) {
                patterns.addAll(pattern.getOutputs());
            }
        }
        return new Object[] { LuaConversion.convert(patterns) };
    }

    /**
     * Gets the fluid patterns of this network.
     * @return An array of all the possible output fluids, or null if disconnected.
     * @since 1.0.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    public final Object[] getFluidPatterns() {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        List<FluidStack> patterns = new LinkedList<>();
        for (ICraftingPattern pattern : network.getCraftingManager().getPatterns()) {
            if (!pattern.getFluidOutputs().isEmpty()) {
                patterns.addAll(pattern.getFluidOutputs());
            }
        }
        return new Object[] { LuaConversion.convert(patterns) };
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
    public final Object[] hasPattern(final Map<?, ?> stack) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        ItemStack items = LuaConversion.getItemStack(stack);
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
    public final Object[] hasFluidPattern(final Map<?, ?> stack) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        FluidStack fluid = LuaConversion.getFluidStack(stack, FluidAttributes.BUCKET_VOLUME);
        return new Object[] { network.getCraftingManager().getPattern(fluid) != null };
    }

    /**
     * Schedules an item crafting task.
     * @param args Arguments, consisting of:
     *               * stack: table -- the item to be crafted
     *               * [count: number] -- the number of items to be crafted (overrides count in stack)
     *               * [canSchedule: boolean] -- if false, does not actually start the craft (just reports if it's possible)
     * @return A table describing the crafting task (even if not actually scheduled), or null if disconnected or unable to craft.
     * @throws LuaException The request was malformed.
     * @since 1.0.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    public final Object[] scheduleTask(final IArguments args) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        ItemStack stack = LuaConversion.getItemStack(args.getTable(0));
        int amount = args.optInt(1, 1);
        boolean canSchedule = args.optBoolean(2, true);

        ICalculationResult result = network.getCraftingManager().create(stack, amount);
        if (result.isOk()) {
            ICraftingTask task = result.getTask();
            if (canSchedule) {
                if (task == null) return error("Crafting task unexpectedly null when OK");
                network.getCraftingManager().start(task);
            }
            return new Object[] { LuaConversion.convert(task) };
        }

        switch (result.getType()) {
            case NO_PATTERN: return error("No pattern found for " + stack.getItem().getRegistryName());
            case MISSING: return error("Required inputs missing for " + stack.getItem().getRegistryName());
            default: return error("Crafting error for " + stack.getItem().getRegistryName());
        }
    }

    /**
     * Schedules a fluid crafting task.
     * @param args Arguments, consisting of:
     *               * stack: table -- the fluid to be crafted
     *               * [amount: number] -- the amount of fluid to be crafted (mB)
     *               * [canSchedule: boolean] -- if false, does not actually start the craft (just reports if it's possible)
     * @return A table describing the crafting task (even if not actually scheduled), or null if disconnected or unable to craft.
     * @throws LuaException The request was malformed.
     * @since 1.0.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    public final Object[] scheduleFluidTask(final IArguments args) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        FluidStack stack = LuaConversion.getFluidStack(args.getTable(0), FluidAttributes.BUCKET_VOLUME);
        int amount = args.optInt(1, stack.getAmount());
        boolean canSchedule = args.optBoolean(2, true);

        ICalculationResult result = network.getCraftingManager().create(stack, amount);
        if (result.isOk()) {
            ICraftingTask task = result.getTask();
            if (canSchedule) {
                if (task == null) return error("Crafting task unexpectedly null when OK");
                network.getCraftingManager().start(task);
            }
            return new Object[] { LuaConversion.convert(task) };
        }

        switch (result.getType()) {
            case NO_PATTERN: return error("No pattern found for " + stack.getFluid().getRegistryName());
            case MISSING: return error("Required inputs missing for " + stack.getFluid().getRegistryName());
            default: return error("Crafting error for " + stack.getFluid().getRegistryName());
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
    public final Object[] cancelTask(final Map<?, ?> stack) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        ItemStack items = LuaConversion.getItemStack(stack);

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
    public final Object[] cancelFluidTask(final Map<?, ?> stack) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        FluidStack fluid = LuaConversion.getFluidStack(stack, FluidAttributes.BUCKET_VOLUME);

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
    public final Object[] extractFluid(final IArguments args) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        // First and second argument: fluid and amount.
        FluidStack stack = LuaConversion.getFluidStack(args.getTable(0), args.optInt(1, FluidAttributes.BUCKET_VOLUME));
        // Third argument: which direction to extract to
        Direction direction = getDirection(args, 2);

        // Get the tile-entity on the specified side
        TileEntity targetEntity = network.getWorld().getTileEntity(this.node.getPos().offset(direction));
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
     * @param args Arguments, consisting of:
     *               * stack: table -- the fluid description (e.g. {name="minecraft:lava"})
     *               * [compareNBT: boolean] -- false to ignore NBT
     * @return A table describing the fluid and how much of it is in the network,
     *         or empty if it is not in the network, or null if the network is disconnected.
     * @throws LuaException The fluid description was invalid.
     * @since 1.0.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    public Object[] getFluid(final IArguments args) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        FluidStack stack = LuaConversion.getFluidStack(args.getTable(0), FluidAttributes.BUCKET_VOLUME);
        boolean compareNBT = args.optBoolean(1, true);

        int flags = compareNBT ? IComparer.COMPARE_NBT : 0;

        stack = network.getFluidStorageCache().getList().get(stack, flags);
        if (stack == null) stack = FluidStack.EMPTY;
        return new Object[] { LuaConversion.convert(stack) };
    }

    /**
     * Gets a list of all fluids currently in this network.
     * @return A table array describing all fluids present, or null if the network is disconnected.
     * @since 1.0.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    public final Object[] getFluids() {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        return new Object[] { LuaConversion.convert(network.getFluidStorageCache().getList().getStacks()) };
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
    public final Object[] extractItem(final IArguments args) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        // First argument: the item stack to extract
        ItemStack stack = LuaConversion.getItemStack(args.getTable(0));
        // Second argument: the number of items to extract, at least 1 ...
        int count = args.optInt(1, stack.getCount());
        // Third argument: which direction to extract to
        Direction direction = getDirection(args, 2);

        // Get the tile-entity on the specified side
        TileEntity targetEntity = network.getWorld().getTileEntity(this.node.getPos().offset(direction));
        IItemHandler handler = targetEntity != null ? targetEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite()).resolve().orElse(null) : null;
        if (handler == null) {
            return error("No item container on the given side");
        }

        ItemStack extractedSim = network.extractItem(stack, count, Action.SIMULATE);
        if (extractedSim.isEmpty()) {
            return new Object[] { 0 };
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
     * Gets information about an item from the network.
     * @param args Arguments, consisting of:
     *               * stack: table -- the item description (e.g. {name="minecraft:stone"})
     *               * [compareNBT: boolean] -- false to ignore NBT
     * @return A table describing the item and how much of it is in the network,
     *         or empty if it is not in the network, or null if the network is disconnected.
     * @throws LuaException The item description was invalid.
     * @since 1.0.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    public Object[] getItem(final IArguments args) throws LuaException {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        ItemStack stack = LuaConversion.getItemStack(args.getTable(0));
        boolean compareNBT = args.optBoolean(1, true);

        int flags = compareNBT ? IComparer.COMPARE_NBT : 0;

        stack = network.getItemStorageCache().getList().get(stack, flags);
        if (stack == null) stack = ItemStack.EMPTY;
        return new Object[] { LuaConversion.convert(stack) };
    }

    /**
     * Gets a list of all items currently in this network.
     * @return A table array describing all items present, or null if the network is disconnected.
     * @since 1.0.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
    public final Object[] getItems() {
        INetwork network = this.getNetwork();
        if (!isConnected(network)) {
            return disconnected();
        }

        return new Object[] { LuaConversion.convert(network.getItemStorageCache().getList().getStacks()) };
    }

    /**
     * Gets a list of all connected storage disks and blocks in this network.
     * @return A table describing the connected storage, or null if the network is disconnected.
     * @since 1.0.0
     */
    @Nonnull
    @LuaFunction(mainThread = true)
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
