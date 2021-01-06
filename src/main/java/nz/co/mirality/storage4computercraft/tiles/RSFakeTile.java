package nz.co.mirality.storage4computercraft.tiles;

import net.minecraft.tileentity.TileEntity;
import nz.co.mirality.storage4computercraft.RS4CCRegistry;

public class RSFakeTile extends TileEntity {
    public RSFakeTile() {
        super(RS4CCRegistry.RS_PERIPHERAL_TILE.get());
    }
}
