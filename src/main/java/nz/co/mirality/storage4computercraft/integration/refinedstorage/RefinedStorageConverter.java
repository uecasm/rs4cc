package nz.co.mirality.storage4computercraft.integration.refinedstorage;

import com.refinedmods.refinedstorage.api.autocrafting.ICraftingPattern;
import com.refinedmods.refinedstorage.api.autocrafting.task.ICraftingRequestInfo;
import com.refinedmods.refinedstorage.api.autocrafting.task.ICraftingTask;
import com.refinedmods.refinedstorage.api.util.StackListEntry;
import nz.co.mirality.storage4computercraft.util.LuaConversion;

import java.util.HashMap;
import java.util.Map;

public class RefinedStorageConverter implements LuaConversion.IConverter {
    @Override
    public Object convert(Object value) {
        if (value instanceof StackListEntry) {
            return LuaConversion.convert(((StackListEntry<?>) value).getStack());
        }
        if (value instanceof ICraftingRequestInfo) {
            ICraftingRequestInfo request = (ICraftingRequestInfo) value;
            Map<Object, Object> result = new HashMap<>();
            result.put("item", LuaConversion.convert(request.getItem()));
            result.put("fluid", LuaConversion.convert(request.getFluid()));
            return result;
        }
        if (value instanceof ICraftingPattern) {
            ICraftingPattern pattern = (ICraftingPattern) value;
            Map<Object, Object> result = new HashMap<>();
            result.put("outputs", LuaConversion.convert(pattern.getOutputs()));
            result.put("inputs", LuaConversion.convert(pattern.getInputs()));

            if (pattern.isProcessing()) {
                result.put("fluidInputs", LuaConversion.convert(pattern.getFluidInputs()));
                result.put("fluidOutputs", LuaConversion.convert(pattern.getFluidOutputs()));
            } else {
                result.put("byproducts", LuaConversion.convert(pattern.getByproducts()));
            }

            result.put("processing", pattern.isProcessing());
            return result;
        }
        if (value instanceof ICraftingTask) {
            ICraftingTask task = (ICraftingTask) value;
            Map<Object, Object> result = new HashMap<>();
            result.put("stack", convert(task.getRequested()));
            result.put("pattern", convert(task.getPattern()));
            result.put("quantity", task.getQuantity());
            return result;
        }
        return null;
    }
}
