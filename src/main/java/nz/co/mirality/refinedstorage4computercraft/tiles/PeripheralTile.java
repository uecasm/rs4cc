package nz.co.mirality.refinedstorage4computercraft.tiles;

import com.refinedmods.refinedstorage.tile.NetworkNodeTile;
import nz.co.mirality.refinedstorage4computercraft.RS4CCTiles;
import nz.co.mirality.refinedstorage4computercraft.nodes.PeripheralNetworkNode;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class PeripheralTile extends NetworkNodeTile<PeripheralNetworkNode> {
    public PeripheralTile() {
        super(RS4CCTiles.PERIPHERAL);
    }

    @Override
    @Nonnull
    public PeripheralNetworkNode createNode(World world, BlockPos pos) {
        return new PeripheralNetworkNode(world, pos);
    }
}
