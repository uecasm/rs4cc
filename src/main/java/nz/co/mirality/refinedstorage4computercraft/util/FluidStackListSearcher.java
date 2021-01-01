package nz.co.mirality.refinedstorage4computercraft.util;

import com.google.common.collect.ArrayListMultimap;
import com.refinedmods.refinedstorage.api.util.IStackList;
import com.refinedmods.refinedstorage.api.util.StackListEntry;
import com.refinedmods.refinedstorage.apiimpl.util.FluidStackList;
import net.minecraft.fluid.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import nz.co.mirality.refinedstorage4computercraft.RS4CC;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class FluidStackListSearcher extends StackListSearcher<FluidStack> {
    static {
        stacksField = ObfuscationReflectionHelper.findField(FluidStackList.class, "stacks");
    }

    public FluidStackListSearcher(IStackList<FluidStack> list) {
        super(list);

        ArrayListMultimap<Fluid, StackListEntry<FluidStack>> stacks;
        try {
            stacks = (ArrayListMultimap<Fluid, StackListEntry<FluidStack>>) stacksField.get(list);
        } catch (IllegalAccessException e) {
            RS4CC.LOGGER.error("Unexpectedly failed to access internal fluid stack list.", e);
            stacks = null;
        }
        this.stacks = stacks;
    }

    private final ArrayListMultimap<Fluid, StackListEntry<FluidStack>> stacks;
    private static final Field stacksField;

    public List<FluidStack> getStacks(@Nonnull FluidStack stack) {
        List<StackListEntry<FluidStack>> entries = this.stacks != null ? stacks.get(stack.getFluid()) : null;

        if (entries == null) return null;

        List<FluidStack> stacks = new ArrayList<>();
        entries.forEach(entry -> stacks.add(entry.getStack()));
        return stacks;
    }
}
