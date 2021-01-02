package nz.co.mirality.refinedstorage4computercraft.util;

import com.refinedmods.refinedstorage.api.autocrafting.ICraftingPattern;
import com.refinedmods.refinedstorage.api.autocrafting.task.ICraftingRequestInfo;
import com.refinedmods.refinedstorage.api.autocrafting.task.ICraftingTask;
import com.refinedmods.refinedstorage.api.util.IStackList;
import com.refinedmods.refinedstorage.api.util.StackListEntry;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.TableHelper;
import dan200.computercraft.shared.peripheral.generic.data.FluidData;
import dan200.computercraft.shared.peripheral.generic.data.ItemData;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocationException;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import nz.co.mirality.refinedstorage4computercraft.RS4CC;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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
            CompoundNBT tag = stack.getTag();
            if (RS4CC.CONFIG.getAllowJsonTags()) {
                result.put("json", NBTUtil.toText(tag));
            }
            if (RS4CC.CONFIG.getAllowEncodedTags()) {
                result.put("tag", NBTUtil.toBinary(tag));
            }
        }

        return result;
    }

    @Nonnull
    private static Map<Object, Object> convertStack(@Nonnull Map<Object, Object> result, @Nonnull FluidStack stack) {
        FluidData.fill(result, stack);

        if (stack.hasTag()) {
            CompoundNBT tag = stack.getTag();

            // the current version of ComputerCraft doesn't provide the NBT hash on fluids, but it should...
            if (!result.containsKey("nbt")) {
                String hash = NBTUtil.toHash(stack.getTag());
                if (hash != null) {
                    result.put("nbt", hash);
                }
            }

            if (RS4CC.CONFIG.getAllowJsonTags()) {
                result.put("json", NBTUtil.toText(tag));
            }
            if (RS4CC.CONFIG.getAllowEncodedTags()) {
                result.put("tag", NBTUtil.toBinary(tag));
            }
        }

        return result;
    }

    @Nonnull
    public static ItemStack getItemStack(@Nullable Map<?, ?> table, @Nonnull Supplier<IStackList<ItemStack>> allStacks) throws LuaException {
        if (table == null || !table.containsKey("name")) return ItemStack.EMPTY;

        String name = TableHelper.getStringField(table, "name");
        int count = TableHelper.optIntField(table, "count", 1);

        Item item = getRegistryEntry(name, "item", ForgeRegistries.ITEMS);
        ItemStack stack = new ItemStack(item, count);

        stack.setTag(getTag(stack, table, allStacks));

        return stack;
    }

    @Nonnull
    public static FluidStack getFluidStack(@Nullable Map<?, ?> table, int defaultAmount, @Nonnull Supplier<IStackList<FluidStack>> allStacks) throws LuaException {
        if (table == null || !table.containsKey("name")) return FluidStack.EMPTY;

        String name = TableHelper.getStringField(table, "name");
        int amount = TableHelper.optIntField(table, "amount", defaultAmount);

        Fluid fluid = getRegistryEntry(name, "fluid", ForgeRegistries.FLUIDS);
        FluidStack stack = new FluidStack(fluid, amount);

        stack.setTag(getTag(stack, table, allStacks));

        return stack;
    }

    @Nullable
    private static CompoundNBT getTag(ItemStack stack, Map<?, ?> table, Supplier<IStackList<ItemStack>> allStacks) throws LuaException {
        CompoundNBT nbt = parseJson(table);
        if (nbt == null) {
            nbt = parseBinaryTag(table);
            if (nbt == null) {
                nbt = parseNbtHash(stack, table, allStacks);
            }
        }
        return nbt;
    }

    @Nullable
    private static CompoundNBT getTag(FluidStack stack, Map<?, ?> table, Supplier<IStackList<FluidStack>> allStacks) throws LuaException {
        CompoundNBT nbt = parseJson(table);
        if (nbt == null) {
            nbt = parseBinaryTag(table);
            if (nbt == null) {
                nbt = parseNbtHash(stack, table, allStacks);
            }
        }
        return nbt;
    }

    @Nullable
    private static CompoundNBT parseJson(@Nonnull Map<?, ?> table) throws LuaException {
        String json = TableHelper.optStringField(table, "json", null);
        return NBTUtil.fromText(json);
    }

    @Nullable
    private static CompoundNBT parseBinaryTag(@Nonnull Map<?, ?> table) throws LuaException {
        String tag = TableHelper.optStringField(table, "tag", null);
        return NBTUtil.fromBinary(tag);
    }

    @Nullable
    private static CompoundNBT parseNbtHash(@Nonnull ItemStack stack, @Nonnull Map<?, ?> table, @Nonnull Supplier<IStackList<ItemStack>> allStacks) throws LuaException {
        String nbt = TableHelper.optStringField(table, "nbt", null);
        if (nbt == null || nbt.isEmpty()) return null;

        for (StackListEntry<ItemStack> search : allStacks.get().getStacks(stack)) {
            CompoundNBT tag = search.getStack().getTag();
            String hash = NBTUtil.toHash(tag);
            if (nbt.equals(hash)) {
                return tag.copy();
            }
        }

        // we didn't find a matching tag in the available list, so we can't
        // reconstruct the input tag successfully.  however, we do need to
        // generate *some* tag to try to ensure that we don't match anything
        // (unless they're explicitly requesting ignoring NBT later on).
        CompoundNBT tag = new CompoundNBT();
        tag.put("_rsPfake_", IntNBT.valueOf(424242));
        return tag;
    }

    @Nullable
    private static CompoundNBT parseNbtHash(@Nonnull FluidStack stack, @Nonnull Map<?, ?> table, @Nonnull Supplier<IStackList<FluidStack>> allStacks) throws LuaException {
        String nbt = TableHelper.optStringField(table, "nbt", null);
        if (nbt == null || nbt.isEmpty()) return null;

        for (StackListEntry<FluidStack> search : allStacks.get().getStacks(stack)) {
            CompoundNBT tag = search.getStack().getTag();
            String hash = NBTUtil.toHash(tag);
            if (nbt.equals(hash)) {
                return tag.copy();
            }
        }

        // we didn't find a matching tag in the available list, so we can't
        // reconstruct the input tag successfully.  however, we do need to
        // generate *some* tag to try to ensure that we don't match anything
        // (unless they're explicitly requesting ignoring NBT later on).
        CompoundNBT tag = new CompoundNBT();
        tag.put("_rsPfake_", IntNBT.valueOf(424242));
        return tag;
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
