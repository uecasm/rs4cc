package nz.co.mirality.storage4computercraft.integration.ae2;

import appeng.api.config.PowerUnits;
import appeng.api.exceptions.FailedConnectionException;
import appeng.api.networking.*;
import appeng.api.networking.events.MENetworkPowerIdleChange;
import appeng.api.util.AEColor;
import appeng.api.util.DimensionalCoord;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.text.TranslationTextComponent;
import nz.co.mirality.storage4computercraft.RS4CC;
import nz.co.mirality.storage4computercraft.RS4CCConfig;
import nz.co.mirality.storage4computercraft.RS4CCRegistry;
import nz.co.mirality.storage4computercraft.util.ServerWorker;
import nz.co.mirality.storage4computercraft.util.Platform;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class MEPeripheralGrid {
    private final MEPeripheralTile tile;
    private final List<GridBlock> grids;
    private CompoundNBT data;

    // we don't save this, under the assumption that computers will reconnect on each load
    private int count;

    public MEPeripheralGrid(final MEPeripheralTile tile) {
        this.tile = tile;
        this.grids = new ArrayList<>();
    }

    public MEPeripheralTile getTile() {
        return this.tile;
    }

    public void validate() {
        // minecraft stalls world loading if we call node.updateState
        // during the initial chunk load -- and we probably don't want
        // to do that anyway, to ensure that adjacent chunks are loaded
        // when checking network connections.  so we defer it until later.
        ServerWorker.add(this::updateGrids);
    }

    public void remove() {
        this.grids.forEach(GridBlock::destroyNode);
        this.grids.clear();
    }

    public boolean isOnline() {
        // we're only online if *all* of our channels are (there should always be at least one)
        return this.grids.stream().allMatch(GridBlock::isConnected);
    }

    public boolean isOverloaded() {
        // we're overloaded if some of our grids have a channel but others don't
        return !this.isOnline() && this.grids.stream().anyMatch(GridBlock::isConnected);
    }

    public int getComputerCount() {
        return this.count;
    }

    public void computerConnected() {
        ++this.count;
        this.updateGrids();
    }

    public void computerDisconnected() {
        this.count = Math.max(0, this.count - 1);
        // clip to zero, just for the sake of sanity
        this.updateGrids();
    }

    @Nonnull
    public CompoundNBT writeToNBT(@Nonnull CompoundNBT tag) {
        final CompoundNBT grids = new CompoundNBT();
        int i = 0;
        for (GridBlock grid : this.grids) {
            grid.writeToNBT(String.valueOf(i), grids);
            ++i;
        }
        tag.put("grids", grids);
        return tag;
    }

    public void readFromNBT(@Nonnull CompoundNBT tag) {
        CompoundNBT grids = tag.getCompound("grids");
        this.data = grids;
        int i = 0;
        for (GridBlock grid : this.grids) {
            grid.readFromNBT(String.valueOf(i), grids);
            ++i;
        }
    }

    private void updateGrids() {
        if (Platform.isClient()) return;    // nodes only exist on server

        // in order to require multiple channels, we model one "primary" node
        // that requires no channels (for the purpose of ensuring that cable
        // connections work), plus N "extra" nodes that each require a channel.
        // note that we can end up requiring more channels than can actually be
        // provided even by a dense cable.  that's ok, we'll just go offline
        // and they'll have to scale the system back (or fix the config).
        final RS4CCConfig.MEPeripheral config = RS4CC.CONFIG.getMEPeripheral();
        final int channels = 1 + config.getBaseChannelUsage() + (config.getPerComputerChannelUsage() * this.count);
        assert channels >= 1;
        final boolean firstTime = this.grids.isEmpty();

        RS4CC.LOGGER.info(String.format("Updating grid to %d nodes (and one less channel)", channels));

        final IGridHelper gridApi = ((AppliedEnergistics) RS4CC.ME_API).getApi().grid();
        for (int i = this.grids.size() - 1; i >= channels; --i) {
            this.grids.get(i).destroyNode();
            this.grids.remove(i);
        }
        for (int i = this.grids.size(); i < channels; ++i) {
            GridBlock grid = new GridBlock(gridApi, i == 0);
            this.grids.add(grid);
            if (this.data != null) {
                grid.readFromNBT(String.valueOf(i), this.data);
            }
            if (i > 0) {
                try {
                    gridApi.createGridConnection(this.grids.get(0).node, this.grids.get(i).node);
                } catch (FailedConnectionException e) {
                    RS4CC.LOGGER.error("Unexpectedly failed to make internal grid connection " + String.valueOf(i), e);
                    // we shouldn't need to do much else, the node will never be online
                }
            }
            grid.node.updateState();
        }
        this.data = null;

        if (!firstTime) {
            // update energy usage etc
            final IGridNode node = this.grids.get(0).node;
            node.getGrid().postEvent(new MENetworkPowerIdleChange(node));
        }
    }

    private double getEnergyUsage() {
        final RS4CCConfig.MEPeripheral config = RS4CC.CONFIG.getMEPeripheral();
        int usage = config.getBaseEnergyUsage();
        if (this.isOnline()) {
            usage += config.getPerComputerEnergyUsage() * this.count;
        }
        return PowerUnits.RF.convertTo(PowerUnits.AE, usage);
    }

    @Nonnull
    private ItemStack getRepresentation() {
        ItemStack stack = new ItemStack(RS4CCRegistry.ME_PERIPHERAL_ITEM.get(), 1);
        stack.setDisplayName(new TranslationTextComponent(stack.getTranslationKey() + ".ui"));
        return stack;
    }

    @Nullable
    public IGridNode getGridNode() {
        return this.grids.isEmpty() ? null : this.grids.get(0).node;
    }

    @Nullable
    public IGrid getOnlineGrid() {
        if (!this.isOnline()) return null;
        IGridNode node = this.getGridNode();
        return node == null ? null : node.getGrid();
    }

    private class GridBlock implements IGridBlock {
        private final boolean isPrimary;
        private final EnumSet<GridFlags> flags;
        private final EnumSet<Direction> validSides;
        private final IGridNode node;

        public GridBlock(IGridHelper gridApi, boolean isPrimary) {
            this.isPrimary = isPrimary;
            this.flags = isPrimary ? EnumSet.of(GridFlags.DENSE_CAPACITY) : EnumSet.of(GridFlags.REQUIRE_CHANNEL);
            this.validSides = EnumSet.allOf(Direction.class);
            this.node = gridApi.createGridNode(this);
        }

        public boolean isConnected() {
            return this.node.isActive();
        }

        public void destroyNode() {
            this.node.destroy();
        }

        public void writeToNBT(@Nonnull final String name, @Nonnull final CompoundNBT tag) {
            this.node.saveToNBT(name, tag);
        }

        public void readFromNBT(@Nonnull final String name, @Nonnull final CompoundNBT tag) {
            if (tag.contains(name)) {
                this.node.loadFromNBT(name, tag);
            }
        }

        @Override
        public double getIdlePowerUsage() {
            return isPrimary ? MEPeripheralGrid.this.getEnergyUsage() : 0;
        }

        @Nonnull
        @Override
        public EnumSet<GridFlags> getFlags() {
            return this.flags;
        }

        @Override
        public boolean isWorldAccessible() {
            return this.isPrimary;
        }

        @Nonnull
        @Override
        public DimensionalCoord getLocation() {
            return new DimensionalCoord(tile);
        }

        @Nonnull
        @Override
        public AEColor getGridColor() {
            return AEColor.TRANSPARENT;
        }

        @Override
        public void onGridNotification(@Nonnull GridNotification gridNotification) {
            if (this.isPrimary && gridNotification == GridNotification.CONNECTIONS_CHANGED) {
                MEPeripheralGrid.this.tile.updateStatus();
            }
        }

        @Nonnull
        @Override
        public EnumSet<Direction> getConnectableSides() {
            return this.validSides;
        }

        @Nonnull
        @Override
        public IGridHost getMachine() {
            return MEPeripheralGrid.this.tile;
        }

        @Override
        public void gridChanged() {

        }

        @Nonnull
        @Override
        public ItemStack getMachineRepresentation() {
            return this.isPrimary ? MEPeripheralGrid.this.getRepresentation() : ItemStack.EMPTY;
        }
    }
}
