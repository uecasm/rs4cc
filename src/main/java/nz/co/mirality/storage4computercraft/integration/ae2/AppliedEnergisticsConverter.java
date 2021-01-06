package nz.co.mirality.storage4computercraft.integration.ae2;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import nz.co.mirality.storage4computercraft.RS4CC;
import nz.co.mirality.storage4computercraft.util.LuaConversion;
import nz.co.mirality.storage4computercraft.util.StackConverter;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AppliedEnergisticsConverter implements LuaConversion.IConverter {
    @Override
    public Object convert(Object value) {
        if (value instanceof IAEItemStack) {
            return convertStack((IAEItemStack) value);
        }
        if (value instanceof IAEFluidStack) {
            return convertStack((IAEFluidStack) value);
        }

        if (value instanceof ICraftingPatternDetails) {
            final ICraftingPatternDetails pattern = (ICraftingPatternDetails) value;
            // the IAEItemStacks don't add any interesting information --
            // in particular the "craftable" is always false, which is confusing
            final List<ItemStack> outputs = pattern.getOutputs().stream().map(AppliedEnergistics::getItemStack).collect(Collectors.toList());
            final List<ItemStack> inputs = pattern.getInputs().stream().map(AppliedEnergistics::getItemStack).collect(Collectors.toList());

            final Map<Object, Object> result = new HashMap<>();
            result.put("outputs", LuaConversion.convert(outputs));
            result.put("inputs", LuaConversion.convert(inputs));

            result.put("processing", !pattern.isCraftable());
            if (pattern.isCraftable()) {
                result.put("exact", !pattern.canSubstitute());
            }
            result.put("priority", pattern.getPriority());
            return result;
        }

        if (value instanceof ICraftingCPU) {
            final ICraftingCPU cpu = (ICraftingCPU) value;
            final ITextComponent name = cpu.getName();

            final Map<Object, Object> result = new HashMap<>();
            result.put("name", name == null ? null : name.getString());
            result.put("storage", cpu.getAvailableStorage());
            result.put("coprocessors", cpu.getCoProcessors());
            result.put("busy", cpu.isBusy());
            return result;
        }

        if (value instanceof ICraftingJob) {
            final ICraftingJob job = (ICraftingJob) value;
            IItemList<IAEItemStack> plan = getApi().getItemStorageChannel().createList();
            job.populatePlan(plan);

            final List<Map<Object, Object>> items = new ArrayList<>(plan.size());
            for (IAEItemStack aeStack : plan) {
                final Map<Object, Object> info = StackConverter.convertStack(AppliedEnergistics.getItemStack(aeStack));
                info.put("crafting", aeStack.getCountRequestable());
                items.add(info);
            }

            final Map<Object, Object> result = new HashMap<>();
            final Map<Object, Object> stack = new HashMap<>();
            stack.put("item", LuaConversion.convert(AppliedEnergistics.getItemStack(job.getOutput())));
            result.put("stack", stack);
            result.put("steps", items);
            result.put("bytes", job.getByteTotal());
            return result;
        }

        return null;
    }

    public static Map<Object, Object> convertStack(IAEItemStack stack) {
        Map<Object, Object> result = new HashMap<>();
        result.put("item", LuaConversion.convert(AppliedEnergistics.getItemStack(stack)));
        result.put("requestable", stack.getCountRequestable());
        result.put("craftable", stack.isCraftable());
        return result;
    }

    public static Map<Object, Object> convertStack(IAEFluidStack stack) {
        Map<Object, Object> result = new HashMap<>();
        result.put("fluid", LuaConversion.convert(AppliedEnergistics.getFluidStack(stack)));
        result.put("requestable", stack.getCountRequestable());
        result.put("craftable", stack.isCraftable());   // they never are, but meh
        return result;
    }

    @Nonnull
    private static AppliedEnergistics getApi() {
        return (AppliedEnergistics) RS4CC.ME_API;
    }
}
