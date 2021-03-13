package nz.co.mirality.storage4computercraft.integration.refinedstorage;

import com.refinedmods.refinedstorage.tile.NetworkNodeTile;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import nz.co.mirality.storage4computercraft.RS4CC;
import nz.co.mirality.storage4computercraft.RS4CCRegistry;
import nz.co.mirality.storage4computercraft.blocks.RSPeripheralBlock;
import nz.co.mirality.storage4computercraft.integration.IProbeFormatting;
import nz.co.mirality.storage4computercraft.integration.IProbeable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static dan200.computercraft.shared.Capabilities.CAPABILITY_PERIPHERAL;

public class RSPeripheralTile extends NetworkNodeTile<RSPeripheralNetworkNode> implements IProbeable {
    public RSPeripheralTile() {
        super(RS4CCRegistry.RS_PERIPHERAL_TILE.get());

        this.peripheral = new RSPeripheral(this);
    }

    private final RSPeripheral peripheral;
    private LazyOptional<IPeripheral> peripheralCap;

    @Override
    @Nonnull
    public RSPeripheralNetworkNode createNode(World world, BlockPos pos) {
        return new RSPeripheralNetworkNode(world, pos);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction direction) {
        if (cap == CAPABILITY_PERIPHERAL) {
            if (peripheralCap == null) {
                peripheralCap = LazyOptional.of(() -> peripheral);
            }
            return peripheralCap.cast();
        }

        return super.getCapability(cap, direction);
    }

    @Override
    protected void invalidateCaps() {
        super.invalidateCaps();

        if (peripheralCap != null) {
            peripheralCap.invalidate();
        }
    }

    @Nonnull
    @Override
    public List<ITextComponent> getProbeData(@Nullable BlockState blockState, @Nonnull IProbeFormatting fmt) {
        final String key = "probe." + RS4CC.ID;
        List<ITextComponent> data = new ArrayList<>();
        data.add(fmt.labelAndInfo(fmt.label(fmt.translate(key + ".computers")),
                fmt.fixed(String.valueOf(this.getNode().getComputerCount()))));
        if (blockState != null) {
            if (blockState.getValue(RSPeripheralBlock.CONNECTED)) {
                data.add(fmt.good(fmt.translate(key + ".network.connected")));
            } else {
                data.add(fmt.warning(fmt.translate(key + ".network.disconnected")));
            }
        }
        return data;
    }
}
