package nz.co.mirality.storage4computercraft.data;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.loot.ILootSerializer;
import net.minecraft.loot.LootConditionType;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.util.JSONUtils;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nonnull;

// this is just here to prevent the loot table registration getting annoyed that
// we don't register the peripherals if their corresponding mod isn't loaded.
public class ModLoadedLootCondition implements ILootCondition {
    public static final LootConditionType TYPE = new LootConditionType(Serializer.INSTANCE);

    private final String modid;

    public ModLoadedLootCondition(String modid) {
        this.modid = modid;
    }

    @Override
    public boolean test(LootContext lootContext) {
        return ModList.get().isLoaded(modid);
    }

    @Override
    public LootConditionType func_230419_b_() {
        return TYPE;
    }

    public static class Serializer implements ILootSerializer<ModLoadedLootCondition> {
        public static final Serializer INSTANCE = new Serializer();

        public void serialize(@Nonnull JsonObject json, @Nonnull ModLoadedLootCondition object, @Nonnull JsonSerializationContext context) {
            json.addProperty("modid", object.modid);
        }

        @Nonnull
        public ModLoadedLootCondition deserialize(@Nonnull JsonObject json, @Nonnull JsonDeserializationContext context) {
            return new ModLoadedLootCondition(JSONUtils.getString(json, "modid"));
        }
    }
}
