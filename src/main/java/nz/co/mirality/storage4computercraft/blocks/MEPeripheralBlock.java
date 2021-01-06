package nz.co.mirality.storage4computercraft.blocks;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.common.ToolType;
import nz.co.mirality.storage4computercraft.RS4CC;
import nz.co.mirality.storage4computercraft.RS4CCRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MEPeripheralBlock extends Block {
    public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");
    public static final BooleanProperty OVERLOADED = BooleanProperty.create("overloaded");

    public MEPeripheralBlock() {
        super(AbstractBlock.Properties.create(Material.IRON)
                .hardnessAndResistance(2.2f, 11.f)
                .harvestTool(ToolType.PICKAXE).harvestLevel(0)
                .sound(SoundType.METAL));

        this.setDefaultState(this.getStateContainer().getBaseState()
                .with(CONNECTED, false).with(OVERLOADED, false));
    }

    @Override
    public boolean hasTileEntity(BlockState state) { return RS4CC.ME_API != null; }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return RS4CCRegistry.ME_PERIPHERAL_TILE.get().create();
    }

    @Override
    protected void fillStateContainer(@Nonnull StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);

        builder.add(CONNECTED).add(OVERLOADED);
    }
}
