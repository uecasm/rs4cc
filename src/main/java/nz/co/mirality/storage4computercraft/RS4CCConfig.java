package nz.co.mirality.storage4computercraft;

import net.minecraftforge.common.ForgeConfigSpec;

public class RS4CCConfig {
    private final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    private final ForgeConfigSpec spec;

    private final RSPeripheral rs_peripheral;
    private final MEPeripheral me_peripheral;
    private final Tags tags;

    public RS4CCConfig() {
        rs_peripheral = new RSPeripheral();
        me_peripheral = new MEPeripheral();
        tags = new Tags();

        spec = builder.build();
    }

    public ForgeConfigSpec getSpec() { return spec; }

    public RSPeripheral getRSPeripheral() { return rs_peripheral; }
    public MEPeripheral getMEPeripheral() { return me_peripheral; }
    public Tags getTags() { return tags; }

    public class RSPeripheral {
        private final ForgeConfigSpec.IntValue baseUsage;
        private final ForgeConfigSpec.IntValue perComputerUsage;

        public RSPeripheral() {
            builder.push("rs_peripheral");

            baseUsage = builder.comment("The base RF/t energy used by the RS Peripheral")
                    .translation("config.storage4computercraft.rs_peripheral.baseUsage")
                    .defineInRange("baseUsage", 10, 0, Integer.MAX_VALUE);
            perComputerUsage = builder.comment("The RF/t energy used by the RS Peripheral for each connected Computer/Turtle")
                    .translation("config.storage4computercraft.rs_peripheral.perComputerUsage")
                    .defineInRange("perComputerUsage", 50, 0, Integer.MAX_VALUE);

            builder.pop();
        }

        public int getBaseUsage() { return baseUsage.get(); }
        public int getPerComputerUsage() { return perComputerUsage.get(); }
    }

    public class MEPeripheral {
        private final ForgeConfigSpec.IntValue baseEnergyUsage;
        private final ForgeConfigSpec.IntValue perComputerEnergyUsage;
        private final ForgeConfigSpec.IntValue baseChannelUsage;
        private final ForgeConfigSpec.IntValue perComputerChannelUsage;

        public MEPeripheral() {
            builder.push("me_peripheral");

            baseEnergyUsage = builder.comment("The base RF/t energy used by the ME Peripheral")
                    .translation("config.storage4computercraft.me_peripheral.baseEnergyUsage")
                    .defineInRange("baseEnergyUsage", 10, 0, Integer.MAX_VALUE);
            perComputerEnergyUsage = builder.comment("The RF/t energy used by the ME Peripheral for each connected Computer/Turtle")
                    .translation("config.storage4computercraft.me_peripheral.perComputerEnergyUsage")
                    .defineInRange("perComputerEnergyUsage", 30, 0, Integer.MAX_VALUE);
            baseChannelUsage = builder.comment("The number of channels to use when idle")
                    .translation("config.storage4computercraft.me_peripheral.baseChannelUsage")
                    .defineInRange("baseChannelUsage", 0, 0, 32);
            perComputerChannelUsage = builder.comment("The number of channels to use for each connected computer")
                    .translation("config.storage4computercraft.me_peripheral.baseChannelUsage")
                    .defineInRange("perComputerChannelUsage", 8, 0, 32);

            builder.pop();
        }

        public int getBaseEnergyUsage() { return baseEnergyUsage.get(); }
        public int getPerComputerEnergyUsage() { return perComputerEnergyUsage.get(); }
        public int getBaseChannelUsage() { return baseChannelUsage.get(); }
        public int getPerComputerChannelUsage() { return perComputerChannelUsage.get(); }
    }

    public class Tags {
        private final ForgeConfigSpec.BooleanValue allowJson;
        private final ForgeConfigSpec.BooleanValue allowEncoded;

        public Tags() {
            builder.push("tags");

            allowJson = builder.comment("If true, exposes tag data as plain JSON.  While enabling powerful sorting options, this has the potential to break the proper progression systems of some mods, so don't enable it lightly.")
                    .translation("config.storage4computercraft.tags.allowJson")
                    .define("allowJson", false);
            allowEncoded = builder.comment("If true, exposes tag data as an encoded blob.  This is less risky than JSON tags but could still be reverse-engineered by the sufficiently determined.")
                    .translation("config.storage4computercraft.tags.allowEncoded")
                    .define("allowEncoded", false);

            builder.pop();
        }

        public boolean getAllowJson() { return allowJson.get(); }
        public boolean getAllowEncoded() { return allowEncoded.get(); }
    }
}
