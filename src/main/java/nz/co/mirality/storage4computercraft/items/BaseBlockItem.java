package nz.co.mirality.storage4computercraft.items;

import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BaseBlockItem extends BlockItem {
    private final Block blockType;
    private boolean enabled;

    public BaseBlockItem(@Nonnull final Block type, @Nonnull Item.Properties props) {
        super(type, props);
        this.blockType = type;
        this.enabled = false;
    }

    public void setEnabled(boolean value) {
        this.enabled = value;
    }

    @Override
    protected boolean allowdedIn(@Nonnull ItemGroup group) {
        if (!enabled) return false;

        return super.allowdedIn(group);
    }

    @Override
    public boolean isBookEnchantable(@Nonnull final ItemStack stack, @Nonnull final ItemStack book) {
        return false;
    }

    @Override
    @Nonnull
    public String getDescriptionId(@Nonnull final ItemStack stack) {
        return this.blockType.getDescriptionId();
    }

    @Override
    @Nonnull
    public ActionResultType place(@Nonnull final BlockItemUseContext context) {
        PlayerEntity player = context.getPlayer();

        ActionResultType result = super.place(context);
        if (!result.consumesAction()) {
            return result;
        }

//        if (this.blockType instanceof AEBaseTileBlock) {
//            TileEntity tile = context.getWorld().getTileEntity(context.getPos());
//
//            if (tile instanceof IGridProxyable) {
//                ((IGridProxyable) tile).getProxy().setOwner(player);
//            }
//
//            //tile.onPlacement(context);
//        }

        return result;

    }
}
