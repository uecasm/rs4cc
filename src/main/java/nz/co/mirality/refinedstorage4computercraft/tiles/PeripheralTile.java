package nz.co.mirality.refinedstorage4computercraft.tiles;

import com.refinedmods.refinedstorage.tile.NetworkNodeTile;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import nz.co.mirality.refinedstorage4computercraft.RS4CCTiles;
import nz.co.mirality.refinedstorage4computercraft.RefinedStoragePeripheral;
import nz.co.mirality.refinedstorage4computercraft.nodes.PeripheralNetworkNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static dan200.computercraft.shared.Capabilities.CAPABILITY_PERIPHERAL;

public class PeripheralTile extends NetworkNodeTile<PeripheralNetworkNode> {
    public PeripheralTile() {
        super(RS4CCTiles.PERIPHERAL);
        peripheral = new RefinedStoragePeripheral(this);
    }

    private final RefinedStoragePeripheral peripheral;
    private LazyOptional<IPeripheral> peripheralCap;

    @Override
    @Nonnull
    public PeripheralNetworkNode createNode(World world, BlockPos pos) {
        return new PeripheralNetworkNode(world, pos);
    }

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
}
