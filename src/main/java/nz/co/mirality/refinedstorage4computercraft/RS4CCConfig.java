package nz.co.mirality.refinedstorage4computercraft;

import net.minecraftforge.common.ForgeConfigSpec;

public class RS4CCConfig {
    private final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    private final ForgeConfigSpec spec;

    private final Peripheral peripheral;
    private final Tags tags;

    public RS4CCConfig() {
        peripheral = new Peripheral();
        tags = new Tags();

        spec = builder.build();
    }

    public ForgeConfigSpec getSpec() { return spec; }

    public Peripheral getPeripheral() { return peripheral; }
    public Tags getTags() { return tags; }

    public class Peripheral {
        private final ForgeConfigSpec.IntValue baseUsage;
        private final ForgeConfigSpec.IntValue perComputerUsage;

        public Peripheral() {
            builder.push("peripheral");

            baseUsage = builder.comment("The base energy used by the Peripheral")
                    .translation("config.refinedstorage4computercraft.peripheral.baseUsage")
                    .defineInRange("baseUsage", 10, 0, Integer.MAX_VALUE);
            perComputerUsage = builder.comment("The energy used by the Peripheral for each connected Computer/Turtle")
                    .translation("config.refinedstorage4computercraft.peripheral.perComputerUsage")
                    .defineInRange("perComputerUsage", 50, 0, Integer.MAX_VALUE);

            builder.pop();
        }

        public int getBaseUsage() { return baseUsage.get(); }
        public int getPerComputerUsage() { return perComputerUsage.get(); }
    }

    public class Tags {
        private final ForgeConfigSpec.BooleanValue allowJson;
        private final ForgeConfigSpec.BooleanValue allowEncoded;

        public Tags() {
            builder.push("tags");

            allowJson = builder.comment("If true, exposes tag data as plain JSON.  While enabling powerful sorting options, this has the potential to break the proper progression systems of some mods, so don't enable it lightly.")
                    .translation("config.refinedstorage4computercraft.tags.allowJson")
                    .define("allowJson", false);
            allowEncoded = builder.comment("If true, exposes tag data as an encoded blob.  This is less risky than JSON tags but could still be reverse-engineered by the sufficiently determined.")
                    .translation("config.refinedstorage4computercraft.tags.allowEncoded")
                    .define("allowEncoded", false);

            builder.pop();
        }

        public boolean getAllowJson() { return allowJson.get(); }
        public boolean getAllowEncoded() { return allowEncoded.get(); }
    }
}
