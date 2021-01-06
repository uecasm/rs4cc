package nz.co.mirality.storage4computercraft.util;

import dan200.computercraft.shared.peripheral.generic.data.FluidData;
import dan200.computercraft.shared.peripheral.generic.data.ItemData;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.fluids.FluidStack;
import nz.co.mirality.storage4computercraft.RS4CC;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class StackConverter implements LuaConversion.IConverter {
    @Override
    public Object convert(Object value) {
        if (value instanceof ItemStack) {
            return convertStack((ItemStack) value);
        }
        if (value instanceof FluidStack) {
            return convertStack((FluidStack) value);
        }
        return null;
    }

    @Nonnull
    public static Map<Object, Object> convertStack(@Nonnull ItemStack stack) {
        Map<Object, Object> result = new HashMap<>();

        boolean wasEmptyish = stack != ItemStack.EMPTY && stack.getCount() == 0;
        if (wasEmptyish) {
            // ordinarily zero-count stacks are treated as empty, which breaks things;
            // we want to treat them as regular stacks where possible
            stack.setCount(1);
        }
        ItemData.fill(result, stack);
        if (wasEmptyish) {
            stack.setCount(0);  // it should be a copy we don't care about, but just in case...
            result.put("count", 0);
        }

        if (stack.hasTag()) {
            CompoundNBT tag = stack.getTag();
            if (RS4CC.CONFIG.getTags().getAllowJson()) {
                result.put("json", NBTUtil.toText(tag));
            }
            if (RS4CC.CONFIG.getTags().getAllowEncoded()) {
                result.put("tag", NBTUtil.toBinary(tag));
            }
        }

        return result;
    }

    @Nonnull
    public static Map<Object, Object> convertStack(@Nonnull FluidStack stack) {
        Map<Object, Object> result = new HashMap<>();
        FluidData.fill(result, stack);
        result.put("displayName", stack.getDisplayName().getString());

        if (stack.hasTag()) {
            CompoundNBT tag = stack.getTag();

            // the current version of ComputerCraft doesn't provide the NBT hash on fluids, but it should...
            if (!result.containsKey("nbt")) {
                String hash = NBTUtil.toHash(stack.getTag());
                if (hash != null) {
                    result.put("nbt", hash);
                }
            }

            if (RS4CC.CONFIG.getTags().getAllowJson()) {
                result.put("json", NBTUtil.toText(tag));
            }
            if (RS4CC.CONFIG.getTags().getAllowEncoded()) {
                result.put("tag", NBTUtil.toBinary(tag));
            }
        }

        return result;
    }
}
