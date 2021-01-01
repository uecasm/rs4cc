package nz.co.mirality.refinedstorage4computercraft.util;

import com.google.common.collect.ArrayListMultimap;
import com.refinedmods.refinedstorage.api.util.IStackList;
import com.refinedmods.refinedstorage.api.util.StackListEntry;
import com.refinedmods.refinedstorage.apiimpl.util.ItemStackList;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import nz.co.mirality.refinedstorage4computercraft.RS4CC;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ItemStackListSearcher extends StackListSearcher<ItemStack> {
    static {
        stacksField = ObfuscationReflectionHelper.findField(ItemStackList.class, "stacks");
    }

    public ItemStackListSearcher(IStackList<ItemStack> list) {
        super(list);

        ArrayListMultimap<Item, StackListEntry<ItemStack>> stacks;
        try {
            stacks = (ArrayListMultimap<Item, StackListEntry<ItemStack>>) stacksField.get(list);
        } catch (IllegalAccessException e) {
            RS4CC.LOGGER.error("Unexpectedly failed to access internal item stack list.", e);
            stacks = null;
        }
        this.stacks = stacks;
    }

    private final ArrayListMultimap<Item, StackListEntry<ItemStack>> stacks;
    private static final Field stacksField;

    public List<ItemStack> getStacks(@Nonnull ItemStack stack) {
        List<StackListEntry<ItemStack>> entries = this.stacks != null ? stacks.get(stack.getItem()) : null;

        if (entries == null) return null;

        List<ItemStack> stacks = new ArrayList<>();
        entries.forEach(entry -> stacks.add(entry.getStack()));
        return stacks;
    }
}
