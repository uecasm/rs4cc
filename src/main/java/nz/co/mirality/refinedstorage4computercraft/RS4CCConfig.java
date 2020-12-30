package nz.co.mirality.refinedstorage4computercraft;

import net.minecraftforge.common.ForgeConfigSpec;

public class RS4CCConfig {
    private final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    private final ForgeConfigSpec spec;

    private final Peripheral peripheral;

    public RS4CCConfig() {
        peripheral = new Peripheral();

        spec = builder.build();
    }

    public ForgeConfigSpec getSpec() { return spec; }

    public Peripheral getPeripheral() { return peripheral; }

    public class Peripheral {
        private final ForgeConfigSpec.IntValue baseUsage;
        private final ForgeConfigSpec.IntValue perComputerUsage;

        public Peripheral() {
            builder.push("peripheral");

            baseUsage = builder.comment("The base energy used by the Peripheral")
                    .defineInRange("baseUsage", 10, 0, Integer.MAX_VALUE);
            perComputerUsage = builder.comment("The energy used by the Peripheral for each connected Computer/Turtle")
                    .defineInRange("perComputerUsage", 50, 0, Integer.MAX_VALUE);

            builder.pop();
        }

        public int getBaseUsage() { return baseUsage.get(); }
        public int getPerComputerUsage() { return perComputerUsage.get(); }
    }
}
