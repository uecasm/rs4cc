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
import nz.co.mirality.storage4computercraft.util.Platform;
import nz.co.mirality.storage4computercraft.util.ServerWorker;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static dan200.computercraft.shared.Capabilities.CAPABILITY_PERIPHERAL;

public class MEPeripheralTile extends TileEntity implements IGridHost, ICraftingRequester, INetworkToolAgent, IProbeable {
    public MEPeripheralTile() {
        super(RS4CCRegistry.ME_PERIPHERAL_TILE.get());

        if (Platform.isServer()) {
            // grids are server-side only
            this.grid = new MEPeripheralGrid(this);
            this.peripheral = new MEPeripheral(this.grid);
        } else {
            this.grid = null;
            this.peripheral = null;
        }
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

        setRemoved();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();

        if (this.grid != null) {
            this.grid.validate();
        }
    }

    @Override
    public void setRemoved() {
        if (this.grid != null) {
            this.grid.remove();
        }

        super.setRemoved();
    }

    @Override
    public void load(@Nonnull BlockState state, @Nonnull CompoundNBT nbt) {
        super.load(state, nbt);

        if (this.grid != null) {
            this.grid.readFromNBT(nbt);
        }
    }

    @Override
    @Nonnull
    public CompoundNBT save(@Nonnull CompoundNBT compound) {
        compound = super.save(compound);

        if (this.grid != null) {
            compound = this.grid.writeToNBT(compound);
        }

        return compound;
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
        if (this.level != null) {
            this.level.destroyBlock(this.worldPosition, true);
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
        return this.grid != null ? this.grid.getGridNode() : null;
    }

    private boolean isDeferredUpdateRequested;
    public void updateStatus() {
        if (this.level == null || this.level.isClientSide() || this.isDeferredUpdateRequested) return;

        this.isDeferredUpdateRequested = true;
        ServerWorker.add(() ->
        {
            this.isDeferredUpdateRequested = false;

            boolean online = this.grid.isOnline();
            boolean overloaded = this.grid.isOverloaded();

            BlockState state = this.level.getBlockState(this.worldPosition);
            if (state.getBlock() == RS4CCRegistry.ME_PERIPHERAL_BLOCK.get()) {
                boolean wasOnline = state.getValue(MEPeripheralBlock.CONNECTED);
                boolean wasOverloaded = state.getValue(MEPeripheralBlock.OVERLOADED);

                if (wasOnline != online || wasOverloaded != overloaded) {
                    this.level.setBlockAndUpdate(this.worldPosition, state
                            .setValue(MEPeripheralBlock.CONNECTED, online)
                            .setValue(MEPeripheralBlock.OVERLOADED, overloaded));
                }
            }
        });
    }

    @Nullable
    @Override
    public IGridNode getGridNode(@Nonnull AEPartLocation aePartLocation) {
        return this.grid != null ? this.grid.getGridNode() : null;
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

        if (this.grid == null) {
            return data;
        }

        data.add(fmt.labelAndInfo(fmt.label(fmt.translate(key + ".computers")),
                fmt.fixed(String.valueOf(this.grid.getComputerCount()))));
        if (blockState != null) {
            if (blockState.getValue(MEPeripheralBlock.OVERLOADED)) {
                data.add(fmt.error(fmt.translate(key + ".network.overloaded")));
            } else if (blockState.getValue(MEPeripheralBlock.CONNECTED)) {
                data.add(fmt.good(fmt.translate(key + ".network.connected")));
            } else {
                data.add(fmt.warning(fmt.translate(key + ".network.disconnected")));
            }
        }
        return data;
    }
}
