package nz.co.mirality.storage4computercraft.integration;

import net.minecraft.nbt.CompoundNBT;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface IStorageSearcher<T> {
    @Nonnull
    List<T> find();

    @Nullable
    T findFirst(@Nonnull T definition, boolean matchTags);

    @Nonnull
    List<T> findIgnoringTags(@Nonnull T definition);
    
    @Nullable
    CompoundNBT findMatchingTagByHash(@Nonnull T definition, @Nonnull String nbtHash);
}
