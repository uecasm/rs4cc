package nz.co.mirality.storage4computercraft.integration.theoneprobe;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import nz.co.mirality.storage4computercraft.RS4CC;
import nz.co.mirality.storage4computercraft.integration.IProbeable;

public class ProbeInfoProvider implements IProbeInfoProvider {
    @Override
    public String getID() {
        return new ResourceLocation(RS4CC.ID, "oneprobe").toString();
    }

    @Override
    public void addProbeInfo(ProbeMode probeMode, IProbeInfo probe, PlayerEntity playerEntity, World world, BlockState blockState, IProbeHitData data) {
        final TileEntity tile = world.getBlockEntity(data.getPos());

        if (tile instanceof IProbeable) {
            for (ITextComponent datum : ((IProbeable) tile).getProbeData(blockState, new ProbeFormatting())) {
                probe.text(datum);
            }
        }
    }
}
