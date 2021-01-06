package nz.co.mirality.storage4computercraft.util;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import nz.co.mirality.storage4computercraft.RS4CC;

import java.util.ArrayDeque;
import java.util.Queue;

@Mod.EventBusSubscriber(modid = RS4CC.ID)
public class ServerWorker {
    private static final Queue<Runnable> serverCallQueue = new ArrayDeque<>();

    public static void add(final Runnable call) {
        serverCallQueue.add(call);
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.END) {
            while (!serverCallQueue.isEmpty()) {
                final Runnable runnable = serverCallQueue.poll();
                runnable.run();
            }
        }
    }
}
