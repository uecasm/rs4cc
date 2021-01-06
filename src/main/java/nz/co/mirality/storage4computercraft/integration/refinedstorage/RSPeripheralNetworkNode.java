package nz.co.mirality.storage4computercraft.integration.refinedstorage;

import com.refinedmods.refinedstorage.apiimpl.network.node.NetworkNode;
import nz.co.mirality.storage4computercraft.RS4CC;
import nz.co.mirality.storage4computercraft.RS4CCConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class RSPeripheralNetworkNode extends NetworkNode {
    public static final ResourceLocation ID = new ResourceLocation(RS4CC.ID, "peripheral");

    public RSPeripheralNetworkNode(World world, BlockPos pos) {
        super(world, pos);
    }

    // we don't save this, under the assumption that computers will reconnect on each load
    private int count;

    public int getComputerCount() {
        return this.count;
    }

    public void computerConnected() {
        ++this.count;
    }

    public void computerDisconnected() {
        this.count = Math.max(0, this.count - 1);
        // clip to zero, just for the sake of sanity
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Nonnull
    @Override
    public ItemStack getItemStack() {
        ItemStack stack = super.getItemStack();
        stack.setDisplayName(new TranslationTextComponent(stack.getTranslationKey() + ".ui"));
        return stack;
    }

    @Override
    public int getEnergyUsage() {
        RS4CCConfig.RSPeripheral config = RS4CC.CONFIG.getRSPeripheral();
        return config.getBaseUsage() + count * config.getPerComputerUsage();
    }
}
