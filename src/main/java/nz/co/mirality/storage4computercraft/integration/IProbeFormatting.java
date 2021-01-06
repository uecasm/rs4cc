package nz.co.mirality.storage4computercraft.integration;

import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public interface IProbeFormatting {
    default IFormattableTextComponent translate(final String key, final Object... args) {
        return new TranslationTextComponent(key, args);
    }

    default IFormattableTextComponent fixed(final String text) {
        return new StringTextComponent(text);
    }

    default IFormattableTextComponent label(final IFormattableTextComponent content) {
        return content.append(fixed("   "));
    }

    ITextComponent good(ITextComponent text);
    ITextComponent info(ITextComponent text);
    ITextComponent warning(ITextComponent text);
    ITextComponent error(ITextComponent text);
    ITextComponent labelAndGood(ITextComponent label, ITextComponent good);
    ITextComponent labelAndWarning(ITextComponent label, ITextComponent warning);
    ITextComponent labelAndInfo(ITextComponent label, ITextComponent info);
    ITextComponent labelAndError(ITextComponent label, ITextComponent error);
}
