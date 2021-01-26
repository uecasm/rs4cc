package nz.co.mirality.storage4computercraft.blocks;

import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;
import nz.co.mirality.storage4computercraft.RS4CC;
import nz.co.mirality.storage4computercraft.RS4CCRegistry;

import javax.annotation.Nullable;

public class RSPeripheralBlock extends BasePeripheralBlock {
    public RSPeripheralBlock() {
        super();
    }

    @Override
    public boolean hasTileEntity(BlockState state) { return RS4CC.RS_API != null; }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return RS4CCRegistry.RS_PERIPHERAL_TILE.get().create();
    }
}