package nz.co.mirality.storage4computercraft.integration.theoneprobe;

import mcjty.theoneprobe.api.ITheOneProbe;

import java.util.function.Function;

public class GotTheOneProbe implements Function<ITheOneProbe, Void> {
    @Override
    public Void apply(ITheOneProbe probe) {
        probe.registerProvider(new ProbeInfoProvider());
        return null;
    }
}
