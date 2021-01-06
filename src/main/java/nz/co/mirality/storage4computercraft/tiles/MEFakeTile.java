package nz.co.mirality.storage4computercraft.tiles;

import net.minecraft.tileentity.TileEntity;
import nz.co.mirality.storage4computercraft.RS4CCRegistry;

public class MEFakeTile extends TileEntity {
    public MEFakeTile() {
        super(RS4CCRegistry.ME_PERIPHERAL_TILE.get());
    }
}
