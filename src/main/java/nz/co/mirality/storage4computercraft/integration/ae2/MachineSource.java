package nz.co.mirality.storage4computercraft.integration.ae2;

import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import net.minecraft.entity.player.PlayerEntity;

import javax.annotation.Nonnull;
import java.util.Optional;

class MachineSource implements IActionSource {
    private final MEPeripheralTile tile;

    public MachineSource(MEPeripheralTile tile) {
        this.tile = tile;
    }

    @Nonnull
    @Override
    public Optional<PlayerEntity> player() {
        return Optional.empty();
    }

    @Nonnull
    @Override
    public Optional<IActionHost> machine() {
        return Optional.of(tile);
    }

    @Nonnull
    @Override
    public <T> Optional<T> context(@Nonnull Class<T> aClass) {
        return Optional.empty();
    }
}
