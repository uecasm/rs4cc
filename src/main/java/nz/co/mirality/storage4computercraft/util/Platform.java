package nz.co.mirality.storage4computercraft.util;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.thread.SidedThreadGroups;
import nz.co.mirality.storage4computercraft.RS4CC;

import java.util.Optional;

public class Platform {
    public static boolean isServer() {
        return Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER;
    }
    public static boolean isClient() {
        return !isServer();
    }

    public static Optional<Object> maybeLoadIntegration(final String modid, final String path) {
        return ModList.get().isLoaded(modid) ? maybeLoadIntegration(path) : Optional.empty();
    }

    public static Optional<Object> maybeLoadIntegration(final String path) {
        try {
            Class<?> clazz = Class.forName(RS4CC.class.getPackage().getName() + ".integration." + path);
            return Optional.of(clazz.newInstance());
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ignored) {
            return Optional.empty();
        }
    }
}
