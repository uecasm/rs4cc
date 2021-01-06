package nz.co.mirality.storage4computercraft.integration.theoneprobe;

import mcjty.theoneprobe.api.CompoundText;
import net.minecraft.util.text.ITextComponent;
import nz.co.mirality.storage4computercraft.integration.IProbeFormatting;

public class ProbeFormatting implements IProbeFormatting {
    @Override
    public ITextComponent good(ITextComponent text) {
        return CompoundText.create().ok(text).get();
    }

    @Override
    public ITextComponent info(ITextComponent text) {
        return CompoundText.create().text(text).get();
    }

    @Override
    public ITextComponent warning(ITextComponent text) {
        return CompoundText.create().warning(text).get();
    }

    @Override
    public ITextComponent error(ITextComponent text) {
        return CompoundText.create().error(text).get();
    }

    @Override
    public ITextComponent labelAndGood(ITextComponent label, ITextComponent good) {
        return CompoundText.create().label(label).ok(good).get();
    }

    @Override
    public ITextComponent labelAndInfo(ITextComponent label, ITextComponent info) {
        return CompoundText.create().label(label).info(info).get();
    }

    @Override
    public ITextComponent labelAndWarning(ITextComponent label, ITextComponent warning) {
        return CompoundText.create().label(label).warning(warning).get();
    }

    @Override
    public ITextComponent labelAndError(ITextComponent label, ITextComponent error) {
        return CompoundText.create().label(label).error(error).get();
    }
}
