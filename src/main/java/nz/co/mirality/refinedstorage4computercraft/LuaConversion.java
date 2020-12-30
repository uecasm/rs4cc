package nz.co.mirality.refinedstorage4computercraft;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.refinedmods.refinedstorage.api.autocrafting.ICraftingPattern;
import com.refinedmods.refinedstorage.api.autocrafting.task.ICraftingRequestInfo;
import com.refinedmods.refinedstorage.api.autocrafting.task.ICraftingTask;
import com.refinedmods.refinedstorage.api.util.StackListEntry;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.TableHelper;
import dan200.computercraft.shared.peripheral.generic.data.FluidData;
import dan200.computercraft.shared.peripheral.generic.data.ItemData;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocationException;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This converts Minecraft values into data suitable for Lua function return values.
 *
 * In an ideal world, we'd have a generic converter registry similar to OpenComputers,
 * but we do not yet live in that world.
 */
public class LuaConversion {
    private LuaConversion() {}

    public static Object convert(Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }

        Map<Object, Object> result = new HashMap<>();
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            map.forEach((k, v) -> result.put(String.valueOf(k), convert(v)));
            return result;
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            for (int i = 0; i < list.size(); ++i) {
                result.put(i + 1, convert(list.get(i)));
            }
            return result;
        }
        if (value instanceof Collection) {
            Collection<?> list = (Collection<?>) value;
            final int[] count = {0};
            list.forEach(v -> result.put(++count[0], convert(v)));
            return result;
        }

        if (value instanceof ItemStack) {
            return convertStack(result, (ItemStack) value);
        }
        if (value instanceof FluidStack) {
            return convertStack(result, (FluidStack) value);
        }
        if (value instanceof StackListEntry) {
            return convert(((StackListEntry<?>) value).getStack());
        }
        if (value instanceof ICraftingRequestInfo) {
            ICraftingRequestInfo request = (ICraftingRequestInfo) value;
            result.put("item", convert(request.getItem()));
            result.put("fluid", convert(request.getFluid()));
            return result;
        }
        if (value instanceof ICraftingPattern) {
            ICraftingPattern pattern = (ICraftingPattern) value;
            result.put("outputs", convert(pattern.getOutputs()));
            result.put("inputs", convert(pattern.getInputs()));

            if (pattern.isProcessing()) {
                result.put("fluidInputs", convert(pattern.getFluidInputs()));
                result.put("fluidOutputs", convert(pattern.getFluidOutputs()));
            } else {
                result.put("byproducts", convert(pattern.getByproducts()));
            }

            result.put("processing", pattern.isProcessing());
            return result;
        }
        if (value instanceof ICraftingTask) {
            ICraftingTask task = (ICraftingTask) value;
            result.put("stack", convert(task.getRequested()));
            result.put("pattern", convert(task.getPattern()));
            result.put("quantity", task.getQuantity());
            return result;
        }

        RS4CC.LOGGER.error("Unable to convert value type {}", value.getClass().getName());
        return result;
    }

    @Nonnull
    private static Map<Object, Object> convertStack(@Nonnull Map<Object, Object> result, @Nonnull ItemStack stack) {
        ItemData.fill(result, stack);

        if (stack.hasTag()) {
            // by default ComputerCraft only puts in a hash of the NBT,
            // which is inadequate to reconstruct the tag for the purposes
            // of IComparer.COMPARE_NBT
            result.put("json", convertNBT(stack.getTag()));
        }
        return result;
    }

    @Nonnull
    public static ItemStack getItemStack(Map<?, ?> table) throws LuaException {
        if (table == null || !table.containsKey("name")) return ItemStack.EMPTY;

        String name = TableHelper.getStringField(table, "name");
        int count = TableHelper.optIntField(table, "count", 1);
        CompoundNBT nbt = parseNBT(TableHelper.optStringField(table, "json", null));

        Item item = getRegistryEntry(name, "item", ForgeRegistries.ITEMS);
        ItemStack stack = new ItemStack(item, count);
        stack.setTag(nbt);
        return stack;
    }

    @Nonnull
    private static Map<Object, Object> convertStack(@Nonnull Map<Object, Object> result, @Nonnull FluidStack stack) {
        FluidData.fill(result, stack);

        if (stack.hasTag()) {
            result.put("json", convertNBT(stack.getTag()));
        }
        return result;
    }

    @Nonnull
    public static FluidStack getFluidStack(Map<?, ?> table, int defaultAmount) throws LuaException {
        if (table == null || !table.containsKey("name")) return FluidStack.EMPTY;

        String name = TableHelper.getStringField(table, "name");
        int amount = TableHelper.optIntField(table, "amount", defaultAmount);
        CompoundNBT nbt = parseNBT(TableHelper.optStringField(table, "json", null));

        Fluid fluid = getRegistryEntry(name, "fluid", ForgeRegistries.FLUIDS);
        FluidStack stack = new FluidStack(fluid, amount);
        stack.setTag(nbt);
        return stack;
    }

    @Nullable
    private static String convertNBT(CompoundNBT nbt) {
        return nbt == null ? null : nbt.toString();
    }

    @Nullable
    private static CompoundNBT parseNBT(String json) {
        try {
            return json == null ? null : JsonToNBT.getTagFromJson(json);
        } catch (CommandSyntaxException e) {
            RS4CC.LOGGER.error("Error parsing NBT data", e);
            return null;
        }
    }

    @Nonnull
    public static <T extends IForgeRegistryEntry<T>> T getRegistryEntry(String name, String typeName, IForgeRegistry<T> registry) throws LuaException {
        ResourceLocation id;
        try {
            id = new ResourceLocation(name);
        } catch (ResourceLocationException ex) {
            id = null;
        }

        T value;
        if (id != null && registry.containsKey(id) && (value = registry.getValue(id)) != null) {
            return value;
        } else {
            throw new LuaException(String.format("Unknown %s '%s'", typeName, name));
        }
    }
}
