package nz.co.mirality.refinedstorage4computercraft;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import nz.co.mirality.refinedstorage4computercraft.tiles.PeripheralTile;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;

public final class RefinedStoragePeripheralProvider
        implements IPeripheralProvider {
    @Nonnull
    @Override
    public final LazyOptional<IPeripheral> getPeripheral(
            @Nonnull World world,
            @Nonnull BlockPos blockPos,
            @Nonnull Direction direction) {
        TileEntity tile = world.getTileEntity(blockPos);
        if (tile instanceof PeripheralTile) {
            return LazyOptional.of(() -> new RefinedStoragePeripheral(((PeripheralTile) tile).getNode()));
        }
        return LazyOptional.empty();
    }
}
