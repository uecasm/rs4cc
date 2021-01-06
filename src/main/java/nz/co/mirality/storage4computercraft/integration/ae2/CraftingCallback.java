package nz.co.mirality.storage4computercraft.integration.ae2;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.*;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.data.IAEItemStack;
import dan200.computercraft.api.lua.ILuaCallback;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import nz.co.mirality.storage4computercraft.RS4CC;
import nz.co.mirality.storage4computercraft.util.LuaConversion;
import nz.co.mirality.storage4computercraft.util.ServerWorker;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class CraftingCallback implements ICraftingCallback, ILuaCallback {
    public static final String COMPLETE = "MEP_scheduleTaskComplete";

    private final IComputerAccess computer;
    private final MEPeripheralGrid grid;
    private final MachineSource source;
    private final Map<?, ?> stack;
    private final Optional<Integer> count;
    private final boolean canSchedule;
    private final String preferredCpu;
    private MethodResult result;
    private LuaException error;

    public CraftingCallback(@Nonnull final IComputerAccess computer, @Nonnull final MEPeripheralGrid grid, @Nonnull final Map<?, ?> stack, @Nonnull final Optional<Integer> count, boolean canSchedule, @Nullable final String preferredCpu) {
        this.computer = computer;
        this.grid = grid;
        this.source = new MachineSource(this.grid.getTile());
        this.stack = stack;
        this.count = count;
        this.canSchedule = canSchedule;
        this.preferredCpu = preferredCpu;
    }

    public void start() {
        // this runs on the server main thread and we're kicking off the crafting calculations
        try {
            IGrid grid = this.grid.getOnlineGrid();
            if (grid == null) {
                this.result = MethodResult.of(null, "not connected");
                this.computer.queueEvent(COMPLETE);
                return;
            }

            final IStorageGrid storage = grid.getCache(IStorageGrid.class);
            final ICraftingGrid crafting = grid.getCache(ICraftingGrid.class);
            ItemStack item = LuaConversion.getItemStack(this.stack, getApi().craftableItems(storage));
            IAEItemStack aeItem = getApi().getAEStack(item);
            if (aeItem == null) {
                this.result = MethodResult.of(null, "No pattern found");
                this.computer.queueEvent(COMPLETE);
                return;
            }
            count.ifPresent(aeItem::setStackSize);

            crafting.beginCraftingJob(this.grid.getTile().getWorld(), grid, this.source, aeItem, this);
            // the task continues in the background...
        } catch (LuaException e) {
            this.error = e;
            this.computer.queueEvent(COMPLETE);
        }
    }

    @Override
    public void calculationComplete(@Nonnull ICraftingJob job) {
        // called on some random internal thread by AE2 once it has calculated
        // the crafting job; it's not safe to actually do any work there.
        ServerWorker.add(() -> doCalculationComplete(job));
    }

    private void doCalculationComplete(@Nonnull ICraftingJob job) {
        // called on the server thread by the above
        if (job.isSimulation()) {
            // this means that the items were not available to actually perform the craft.
            // we could possibly report back some details, but we're not doing that for RS, so...
            this.result = MethodResult.of(null, "Inputs missing");
            this.computer.queueEvent(COMPLETE);
            return;
        }

        // this means that the crafting is possible
        if (!this.canSchedule) {
            // but the caller didn't want to actually start it, so just return the info
            this.result = MethodResult.of(LuaConversion.convert(job));
            this.computer.queueEvent(COMPLETE);
            return;
        }

        // and we want to actually start the job
        IGrid grid = this.grid.getOnlineGrid();
        if (grid == null) {
            // whoops, looks like the grid went down between then and now
            this.result = MethodResult.of(null, "not connected");
            this.computer.queueEvent(COMPLETE);
            return;
        }

        final ICraftingGrid crafting = grid.getCache(ICraftingGrid.class);
        final ICraftingCPU cpu = findCpu(crafting);
        final ICraftingRequester requester = null; //this.grid.getTile();
        final ICraftingLink link = crafting.submitJob(job, requester, cpu, false, this.source);
        if (link == null) {
            // something failed
            this.result = MethodResult.of(null, "Crafting error");
        } else {
            // the crafting has been started; but we're done here
            this.result = MethodResult.of(LuaConversion.convert(job));
        }
        this.computer.queueEvent(COMPLETE);
    }

    @Nonnull
    @Override
    public MethodResult resume(Object[] objects) throws LuaException {
        // this is called back on the Lua thread to actually report the results of
        // our async operation back to the caller.
        if (this.error != null) throw this.error;
        if (this.result != null) return this.result;

        // we should never actually reach this point without having set one of the two above...
        throw new LuaException("unexpected state in " + COMPLETE);
    }

    @Nonnull
    private static AppliedEnergistics getApi() {
        return (AppliedEnergistics) RS4CC.ME_API;
    }

    private ICraftingCPU findCpu(final ICraftingGrid crafting) {
        if (this.preferredCpu != null) {
            for (ICraftingCPU cpu : crafting.getCpus()) {
                ITextComponent name = cpu.getName();
                if (name != null && name.getString().equals(this.preferredCpu)) {
                    return cpu;
                }
            }
        }
        return null;
    }
}
