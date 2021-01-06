package nz.co.mirality.storage4computercraft.integration.refinedstorage;

import net.minecraft.tileentity.TileEntityType;
import nz.co.mirality.storage4computercraft.integration.IStorageSystem;

import java.util.function.Supplier;

public interface IRefinedStorage extends IStorageSystem {
    Supplier<TileEntityType<?>> createPeripheral();
}
