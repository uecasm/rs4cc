package nz.co.mirality.refinedstorage4computercraft;

import nz.co.mirality.refinedstorage4computercraft.tiles.PeripheralTile;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.registries.ObjectHolder;

@ObjectHolder(RS4CC.ID)
public final class RS4CCTiles {
    @ObjectHolder(RS4CC.PERIPHERAL_ID)
    public static final TileEntityType<PeripheralTile> PERIPHERAL = null;

    private RS4CCTiles() {}
}
