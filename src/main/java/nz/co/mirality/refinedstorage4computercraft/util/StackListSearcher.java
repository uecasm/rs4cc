package nz.co.mirality.refinedstorage4computercraft.util;

import com.refinedmods.refinedstorage.api.util.IStackList;
import com.refinedmods.refinedstorage.api.util.StackListEntry;
import com.refinedmods.refinedstorage.api.util.StackListResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.UUID;

public abstract class StackListSearcher<T> implements IStackList<T> {
    protected StackListSearcher(IStackList<T> list) {
        this.list = list;
    }

    protected final IStackList<T> list;

    @Override
    public StackListResult<T> add(@Nonnull T t, int i) {
        return this.list.add(t, i);
    }

    @Override
    public StackListResult<T> add(@Nonnull T t) {
        return this.list.add(t);
    }

    @Override
    @Nullable
    public StackListResult<T> remove(@Nonnull T t, int i) {
        return this.list.remove(t, i);
    }

    @Override
    @Nullable
    public StackListResult<T> remove(@Nonnull T t) {
        return this.list.remove(t);
    }

    @Override
    @Nullable
    public T get(@Nonnull T stack) {
        return this.list.get(stack);
    }

    @Override
    public int getCount(@Nonnull T t, int i) {
        return this.list.getCount(t, i);
    }

    @Override
    public int getCount(@Nonnull T stack) {
        return this.list.getCount(stack);
    }

    @Override
    @Nullable
    public T get(@Nonnull T t, int i) {
        return this.list.get(t, i);
    }

    @Override
    @Nullable
    public StackListEntry<T> getEntry(@Nonnull T t, int i) {
        return this.list.getEntry(t, i);
    }

    @Override
    @Nullable
    public T get(UUID uuid) {
        return this.list.get(uuid);
    }

    @Override
    public void clear() {
        this.list.clear();
    }

    @Override
    public boolean isEmpty() {
        return this.list.isEmpty();
    }

    @Override
    @Nonnull
    public Collection<StackListEntry<T>> getStacks() {
        return this.list.getStacks();
    }

    @Override
    @Nonnull
    public IStackList<T> copy() {
        return this.list.copy();
    }

    @Override
    public int size() {
        return this.list.size();
    }
}

