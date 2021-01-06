package nz.co.mirality.storage4computercraft.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.JsonToNBT;
import nz.co.mirality.storage4computercraft.RS4CC;

import javax.annotation.Nullable;
import java.io.*;
import java.util.Base64;

public class NBTUtil {
    private NBTUtil() {}

    @Nullable
    public static String toText(@Nullable CompoundNBT nbt) {
        return nbt == null ? null : nbt.toString();
    }

    @Nullable
    public static CompoundNBT fromText(@Nullable String json) {
        try {
            return json == null ? null : JsonToNBT.getTagFromJson(json);
        } catch (CommandSyntaxException e) {
            RS4CC.LOGGER.error("Error parsing NBT text data", e);
            return null;
        }
    }

    @Nullable
    public static String toBinary(@Nullable CompoundNBT nbt) {
        if (nbt == null) return null;

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            try (OutputStream stream = Base64.getEncoder().wrap(outputStream)) {
                CompressedStreamTools.writeCompressed(nbt, stream);
            }
            return outputStream.toString();
        } catch (IOException e) {
            RS4CC.LOGGER.error("Error formatting NBT binary data", e);
            return null;
        }
    }

    @Nullable
    public static CompoundNBT fromBinary(@Nullable String base64) {
        if (base64 == null) return null;

        try (InputStream inputStream = Base64.getDecoder().wrap(new ByteArrayInputStream(base64.getBytes()))) {
            return CompressedStreamTools.readCompressed(inputStream);
        } catch (IOException e) {
            RS4CC.LOGGER.error("Error parsing NBT binary data", e);
            return null;
        }
    }

    @Nullable
    public static String toHash(@Nullable CompoundNBT nbt) {
        return dan200.computercraft.shared.util.NBTUtil.getNBTHash(nbt);
    }
}
