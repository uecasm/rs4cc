package nz.co.mirality.storage4computercraft.util;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.TableHelper;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocationException;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import nz.co.mirality.storage4computercraft.RS4CC;
import nz.co.mirality.storage4computercraft.integration.IStorageSearcher;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * This converts Minecraft values into data suitable for Lua function return values.
 */
public class LuaConversion {
    public interface IConverter {
        Object convert(Object value);
    }

    private LuaConversion() {}

    private static final List<IConverter> CONVERTERS = new ArrayList<>();

    public static void register(IConverter converter) {
        CONVERTERS.add(converter);
    }

    static {
        register(new CollectionConverter());
        register(new StackConverter());
    }

    public static Object convert(Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }

        for (IConverter converter : CONVERTERS) {
            Object result = converter.convert(value);
            if (result != null) return result;
        }

        RS4CC.LOGGER.error("Unable to convert value type {}", value.getClass().getName());
        return null;
    }

    private static class CollectionConverter implements IConverter {
        @Override
        public Object convert(Object value) {
            if (value instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) value;
                Map<Object, Object> result = new HashMap<>();
                map.forEach((k, v) -> result.put(String.valueOf(k), LuaConversion.convert(v)));
                return result;
            }
            if (value instanceof List) {
                List<?> list = (List<?>) value;
                Map<Object, Object> result = new HashMap<>();
                for (int i = 0, count = list.size(); i < count; ++i) {
                    result.put(i + 1, LuaConversion.convert(list.get(i)));
                }
                return result;
            }
            if (value instanceof Collection) {
                Collection<?> list = (Collection<?>) value;
                Map<Object, Object> result = new HashMap<>();
                final int[] count = {0};
                list.forEach(v -> result.put(++count[0], LuaConversion.convert(v)));
                return result;
            }
            return null;
        }
    }

    @Nonnull
    public static ItemStack parseZeroItemStack(@Nonnull Map<?, ?> stack) throws LuaException {
        ItemStack item = getBasicItemStack(stack);
        if (item != ItemStack.EMPTY) {
            item.setCount(0);
        }
        return item;
    }

    @Nonnull
    public static FluidStack parseZeroFluidStack(@Nonnull Map<?, ?> stack) throws LuaException {
        FluidStack fluid = getBasicFluidStack(stack);
        if (fluid != FluidStack.EMPTY) {
            fluid.setAmount(0);
        }
        return fluid;
    }

    @Nonnull
    public static ItemStack getBasicItemStack(@Nullable Map<?, ?> table) throws LuaException {
        if (table == null || !table.containsKey("name")) return ItemStack.EMPTY;

        String name = TableHelper.getStringField(table, "name");

        Item item = getRegistryEntry(name, "item", ForgeRegistries.ITEMS);
        return new ItemStack(item, 1);
    }

    @Nonnull
    public static FluidStack getBasicFluidStack(@Nullable Map<?, ?> table) throws LuaException {
        if (table == null || !table.containsKey("name")) return FluidStack.EMPTY;

        String name = TableHelper.getStringField(table, "name");

        Fluid fluid = getRegistryEntry(name, "fluid", ForgeRegistries.FLUIDS);
        return new FluidStack(fluid, FluidAttributes.BUCKET_VOLUME);
    }

    @Nonnull
    public static ItemStack getItemStack(@Nullable Map<?, ?> table, @Nonnull IStorageSearcher<ItemStack> searcher) throws LuaException {
        ItemStack stack = LuaConversion.getBasicItemStack(table);
        if (stack.isEmpty()) return stack;
        assert table != null;

        if (table.containsKey("count")) {
            stack.setCount(TableHelper.getIntField(table, "count"));
        }

        stack.setTag(getTag(stack, table, searcher));

        return stack;
    }

    @Nonnull
    public static FluidStack getFluidStack(@Nullable Map<?, ?> table, @Nonnull IStorageSearcher<FluidStack> searcher) throws LuaException {
        FluidStack stack = LuaConversion.getBasicFluidStack(table);
        if (stack.isEmpty()) return stack;
        assert table != null;

        if (table.containsKey("amount")) {
            stack.setAmount(TableHelper.getIntField(table, "amount"));
        }

        stack.setTag(getTag(stack, table, searcher));

        return stack;
    }

    @Nonnull
    public static List<ItemStack> getItemStacks(@Nullable Map<?, ?> table, @Nonnull IStorageSearcher<ItemStack> searcher) throws LuaException {
        return table == null
                ? searcher.find()
                : searcher.findIgnoringTags(LuaConversion.getBasicItemStack(table));
    }

    @Nonnull
    public static List<FluidStack> getFluidStacks(@Nullable Map<?, ?> table, @Nonnull IStorageSearcher<FluidStack> searcher) throws LuaException {
        return table == null
                ? searcher.find()
                : searcher.findIgnoringTags(LuaConversion.getBasicFluidStack(table));
    }

    @Nullable
    private static CompoundNBT getTag(@Nonnull ItemStack stack, Map<?, ?> table, @Nonnull IStorageSearcher<ItemStack> searcher) throws LuaException {
        CompoundNBT nbt = LuaConversion.parseJson(table);
        if (nbt == null) {
            nbt = LuaConversion.parseBinaryTag(table);
            if (nbt == null) {
                nbt = parseNbtHash(stack, table, searcher);
            }
        }
        return nbt;
    }

    @Nullable
    private static CompoundNBT getTag(@Nonnull FluidStack stack, Map<?, ?> table, @Nonnull IStorageSearcher<FluidStack> searcher) throws LuaException {
        CompoundNBT nbt = LuaConversion.parseJson(table);
        if (nbt == null) {
            nbt = LuaConversion.parseBinaryTag(table);
            if (nbt == null) {
                nbt = parseNbtHash(stack, table, searcher);
            }
        }
        return nbt;
    }

    @Nullable
    public static CompoundNBT parseJson(@Nonnull Map<?, ?> table) throws LuaException {
        String json = TableHelper.optStringField(table, "json", null);
        return NBTUtil.fromText(json);
    }

    @Nullable
    public static CompoundNBT parseBinaryTag(@Nonnull Map<?, ?> table) throws LuaException {
        String tag = TableHelper.optStringField(table, "tag", null);
        return NBTUtil.fromBinary(tag);
    }

    @Nullable
    private static CompoundNBT parseNbtHash(@Nonnull ItemStack stack, @Nonnull Map<?, ?> table, @Nonnull IStorageSearcher<ItemStack> searcher) throws LuaException {
        String nbt = TableHelper.optStringField(table, "nbt", null);
        if (nbt == null || nbt.isEmpty()) return null;

        CompoundNBT tag = searcher.findMatchingTagByHash(stack, nbt);
        if (tag != null) return tag;

        // we didn't find a matching tag in the available list, so we can't
        // reconstruct the input tag successfully.  however, we do need to
        // generate *some* tag to try to ensure that we don't match anything
        // (unless they're explicitly requesting ignoring NBT later on).
        tag = new CompoundNBT();
        tag.put("_rsPfake_", IntNBT.valueOf(424242));
        return tag;
    }

    @Nullable
    private static CompoundNBT parseNbtHash(@Nonnull FluidStack stack, @Nonnull Map<?, ?> table, @Nonnull IStorageSearcher<FluidStack> searcher) throws LuaException {
        String nbt = TableHelper.optStringField(table, "nbt", null);
        if (nbt == null || nbt.isEmpty()) return null;

        CompoundNBT tag = searcher.findMatchingTagByHash(stack, nbt);
        if (tag != null) return tag;

        // we didn't find a matching tag in the available list, so we can't
        // reconstruct the input tag successfully.  however, we do need to
        // generate *some* tag to try to ensure that we don't match anything
        // (unless they're explicitly requesting ignoring NBT later on).
        tag = new CompoundNBT();
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
