package nz.co.mirality.storage4computercraft.integration;

import mcp.mobius.waila.api.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import nz.co.mirality.storage4computercraft.RS4CC;
import nz.co.mirality.storage4computercraft.blocks.MEPeripheralBlock;
import nz.co.mirality.storage4computercraft.blocks.RSPeripheralBlock;

import java.util.List;

@WailaPlugin(RS4CC.ID)
public class Waila implements IWailaPlugin {
    @Override
    public void register(IRegistrar registrar) {
        WailaProvider provider = new WailaProvider();

        registrar.registerBlockDataProvider(provider, RSPeripheralBlock.class);
        registrar.registerBlockDataProvider(provider, MEPeripheralBlock.class);

        registrar.registerComponentProvider(provider, TooltipPosition.TAIL, RSPeripheralBlock.class);
        registrar.registerComponentProvider(provider, TooltipPosition.TAIL, MEPeripheralBlock.class);
    }

    private static class WailaProvider implements IComponentProvider, IServerDataProvider<TileEntity> {
        @Override
        public ItemStack getStack(IDataAccessor accessor, IPluginConfig config) {
            return new ItemStack(accessor.getBlock(), 1);
        }

        @Override
        public void appendTail(List<ITextComponent> tooltip, IDataAccessor accessor, IPluginConfig config) {
            ListNBT data = (ListNBT) accessor.getServerData().get("probedata");
            if (data != null) {
                for (INBT item : data) {
                    tooltip.add(ITextComponent.Serializer.getComponentFromJson(item.getString()));
                }
            }
        }

        @Override
        public void appendServerData(CompoundNBT nbt, ServerPlayerEntity player, World world, TileEntity tile) {
            if (tile instanceof IProbeable) {
                IProbeable probeable = (IProbeable) tile;
                List<ITextComponent> data = probeable.getProbeData(tile.getBlockState(), new ProbeFormatting());
                ListNBT dataNBT = new ListNBT();
                for (ITextComponent item : data) {
                    dataNBT.add(StringNBT.valueOf(ITextComponent.Serializer.toJson(item)));
                }
                nbt.put("probedata", dataNBT);
            }
        }
    }

    private static class ProbeFormatting implements IProbeFormatting {
        @Override
        public ITextComponent good(ITextComponent text) {
            return fixed("\u00a7a").append(text);
        }

        @Override
        public ITextComponent info(ITextComponent text) {
            return fixed("\u00a7f").append(text);
        }

        @Override
        public ITextComponent warning(ITextComponent text) {
            return fixed("\u00a7e").append(text);
        }

        @Override
        public ITextComponent error(ITextComponent text) {
            return fixed("\u00a74").append(text);
        }

        @Override
        public ITextComponent labelAndGood(ITextComponent label, ITextComponent good) {
            return ((IFormattableTextComponent) label).append(good(good));
        }

        @Override
        public ITextComponent labelAndWarning(ITextComponent label, ITextComponent warning) {
            return ((IFormattableTextComponent) label).append(warning(warning));
        }

        @Override
        public ITextComponent labelAndInfo(ITextComponent label, ITextComponent info) {
            return ((IFormattableTextComponent) label).append(info(info));
        }

        @Override
        public ITextComponent labelAndError(ITextComponent label, ITextComponent error) {
            return ((IFormattableTextComponent) label).append(error(error));
        }
    }
}
