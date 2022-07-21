package me.makkuusen.timing.system;

import co.aikar.taskchain.TaskChain;
import me.makkuusen.timing.system.event.EventAnnouncements;
import me.makkuusen.timing.system.heat.Heat;

public class TaskChainCountdown {

    public static void countdown(Heat heat) {
        TaskChain<?> chain = TimingSystem.newSharedChain("COUNTDOWN");
        chain
                .sync(() -> {
                    EventAnnouncements.broadcastCountdown(heat, 5);
                })
                .delay(20)
                .sync(() -> {
                    EventAnnouncements.broadcastCountdown(heat, 4);
                })
                .delay(20)
                .sync(() -> {
                    EventAnnouncements.broadcastCountdown(heat, 3);
                })
                .delay(20)
                .sync(() -> {
                    EventAnnouncements.broadcastCountdown(heat, 2);
                })
                .delay(20)
                .sync(() -> {
                    EventAnnouncements.broadcastCountdown(heat, 1);
                })
                .delay(20)
                .execute((finished) -> {
                    heat.startHeat();
                });
    }
}
