package nz.co.mirality.storage4computercraft.integration.ae2;

import net.minecraft.tileentity.TileEntityType;
import nz.co.mirality.storage4computercraft.integration.IStorageSystem;

import java.util.function.Supplier;

public interface IAppliedEnergistics extends IStorageSystem {
    Supplier<TileEntityType<?>> createPeripheral();
}
