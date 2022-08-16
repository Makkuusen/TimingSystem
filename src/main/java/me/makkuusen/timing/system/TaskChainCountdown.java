package me.makkuusen.timing.system;

import co.aikar.taskchain.TaskChain;
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.heat.Heat;

public class TaskChainCountdown {

    public static void countdown(Heat heat, int count) {
        TaskChain<?> chain = TimingSystem.newChain();

        for (int i = count; i > 0; i--) {
            int finalI = i;
            chain.sync(() -> {
                EventAnnouncements.broadcastCountdown(heat, finalI);
            }).delay(20);
        }
        chain.execute((finished) -> {
            heat.startHeat();
        });
    }
}
