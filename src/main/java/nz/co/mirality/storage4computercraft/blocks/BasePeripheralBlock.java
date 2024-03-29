package nz.co.mirality.storage4computercraft.blocks;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nonnull;

public abstract class BasePeripheralBlock extends Block {
    public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");

    public BasePeripheralBlock() {
        super(AbstractBlock.Properties.of(Material.METAL)
                .strength(2.2f, 11.f)
                .harvestTool(ToolType.PICKAXE).harvestLevel(0)
                .sound(SoundType.METAL));

        this.registerDefaultState(this.getStateDefinition().any().setValue(CONNECTED, false));
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateContainer.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);

        builder.add(CONNECTED);
    }
}
