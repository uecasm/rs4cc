package nz.co.mirality.storage4computercraft.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.events.MENetworkChannelsChanged;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.INetworkToolAgent;
import com.google.common.collect.ImmutableSet;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import nz.co.mirality.storage4computercraft.RS4CC;
import nz.co.mirality.storage4computercraft.RS4CCRegistry;
import nz.co.mirality.storage4computercraft.blocks.MEPeripheralBlock;
import nz.co.mirality.storage4computercraft.integration.IProbeFormatting;
import nz.co.mirality.storage4computercraft.integration.IProbeable;
import nz.co.mirality.storage4computercraft.util.ServerWorker;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static dan200.computercraft.shared.Capabilities.CAPABILITY_PERIPHERAL;

public class MEPeripheralTile extends TileEntity implements IGridHost, ICraftingRequester, INetworkToolAgent, IProbeable {
    public MEPeripheralTile() {
        super(RS4CCRegistry.ME_PERIPHERAL_TILE.get());

        this.grid = new MEPeripheralGrid(this);
        this.peripheral = new MEPeripheral(this.grid);
    }

    private final MEPeripheralGrid grid;
    private final MEPeripheral peripheral;
    private LazyOptional<IPeripheral> peripheralCap;

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction direction) {
        if (cap == CAPABILITY_PERIPHERAL) {
            if (peripheralCap == null) {
                peripheralCap = LazyOptional.of(() -> peripheral);
            }
            return peripheralCap.cast();
        }

        return super.getCapability(cap, direction);
    }

    @Override
    protected void invalidateCaps() {
        super.invalidateCaps();

        if (peripheralCap != null) {
            peripheralCap.invalidate();
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();

        remove();
    }

    @Override
    public void validate() {
        super.validate();

        this.grid.validate();
    }

    @Override
    public void remove() {
        this.grid.remove();

        super.remove();
    }

    @Override
    public void read(@Nonnull BlockState state, @Nonnull CompoundNBT nbt) {
        super.read(state, nbt);

        this.grid.readFromNBT(nbt);
    }

    @Override
    @Nonnull
    public CompoundNBT write(@Nonnull CompoundNBT compound) {
        return this.grid.writeToNBT(super.write(compound));
    }

    @MENetworkEventSubscribe
    public void powerUpdate(final MENetworkPowerStatusChange c) {
        this.updateStatus();
    }

    @MENetworkEventSubscribe
    public void channelUpdate(final MENetworkChannelsChanged c) {
        this.updateStatus();
    }

    public void securityBreak() {
        if (this.world != null) {
            this.world.destroyBlock(this.pos, true);
        }
    }

    @Override
    public IAEItemStack injectCraftedItems(ICraftingLink link, IAEItemStack aeItem, Actionable actionable) {
        // put them straight back into AE storage, rather than an external inventory.
        return aeItem;
    }

    @Override
    public void jobStateChange(ICraftingLink link) {
        // we don't particularly care, we're not directly keeping track of crafting
        // jobs on the computer once they are actually started.
    }

    @Nonnull
    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        // we're not actually keeping track of any previous requests.
        return ImmutableSet.of();
    }

    @Nullable
    @Override
    public IGridNode getActionableNode() {
        return this.grid.getGridNode();
    }

    private boolean isDeferredUpdateRequested;
    public void updateStatus() {
        if (this.world == null || this.world.isRemote() || this.isDeferredUpdateRequested) return;

        this.isDeferredUpdateRequested = true;
        ServerWorker.add(() ->
        {
            this.isDeferredUpdateRequested = false;

            boolean online = this.grid.isOnline();
            boolean overloaded = this.grid.isOverloaded();

            BlockState state = this.world.getBlockState(this.pos);
            if (state.getBlock() == RS4CCRegistry.ME_PERIPHERAL_BLOCK.get()) {
                boolean wasOnline = state.get(MEPeripheralBlock.CONNECTED);
                boolean wasOverloaded = state.get(MEPeripheralBlock.OVERLOADED);

                if (wasOnline != online || wasOverloaded != overloaded) {
                    this.world.setBlockState(this.pos, state
                            .with(MEPeripheralBlock.CONNECTED, online)
                            .with(MEPeripheralBlock.OVERLOADED, overloaded));
                }
            }
        });
    }

    @Nullable
    @Override
    public IGridNode getGridNode(@Nonnull AEPartLocation aePartLocation) {
        return this.grid.getGridNode();
    }

    @Nonnull
    @Override
    public AECableType getCableConnectionType(@Nonnull AEPartLocation aePartLocation) {
        return AECableType.DENSE_SMART;
    }

    @Override
    public boolean showNetworkInfo(RayTraceResult rayTraceResult) {
        return true;
    }

    @Nonnull
    @Override
    public List<ITextComponent> getProbeData(@Nullable BlockState blockState, @Nonnull IProbeFormatting fmt) {
        final String key = "probe." + RS4CC.ID;
        List<ITextComponent> data = new ArrayList<>();
        data.add(fmt.labelAndInfo(fmt.label(fmt.translate(key + ".computers")),
                fmt.fixed(String.valueOf(this.grid.getComputerCount()))));
        if (blockState != null) {
            if (blockState.get(MEPeripheralBlock.OVERLOADED)) {
                data.add(fmt.error(fmt.translate(key + ".network.overloaded")));
            } else if (blockState.get(MEPeripheralBlock.CONNECTED)) {
                data.add(fmt.good(fmt.translate(key + ".network.connected")));
            } else {
                data.add(fmt.warning(fmt.translate(key + ".network.disconnected")));
            }
        }
        return data;
    }
}
