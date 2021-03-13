package nz.co.mirality.storage4computercraft.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;
import nz.co.mirality.storage4computercraft.RS4CC;
import nz.co.mirality.storage4computercraft.RS4CCRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MEPeripheralBlock extends BasePeripheralBlock {
    public static final BooleanProperty OVERLOADED = BooleanProperty.create("overloaded");

    public MEPeripheralBlock() {
        super();

        this.registerDefaultState(this.defaultBlockState()
                .setValue(OVERLOADED, false));
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateContainer.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);

        builder.add(OVERLOADED);
    }

    @Override
    public boolean hasTileEntity(BlockState state) { return RS4CC.ME_API != null; }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return RS4CCRegistry.ME_PERIPHERAL_TILE.get().create();
    }
}
