package nz.co.mirality.refinedstorage4computercraft.blocks;

import com.refinedmods.refinedstorage.block.NetworkNodeBlock;
import com.refinedmods.refinedstorage.util.BlockUtils;
import nz.co.mirality.refinedstorage4computercraft.tiles.PeripheralTile;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;

import javax.annotation.Nullable;

public class PeripheralBlock extends NetworkNodeBlock {
    public PeripheralBlock() {
        super(BlockUtils.DEFAULT_ROCK_PROPERTIES);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new PeripheralTile();
    }

    @Override
    public boolean hasConnectedState() {
        return true;
    }
}