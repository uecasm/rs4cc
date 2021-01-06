package nz.co.mirality.storage4computercraft.integration;

import net.minecraft.block.BlockState;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface IProbeable {
    @Nonnull
    List<ITextComponent> getProbeData(@Nullable BlockState blockState, @Nonnull IProbeFormatting formatter);
}

