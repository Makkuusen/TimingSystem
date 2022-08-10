package me.makkuusen.timing.system.round;

import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.event.Event;

public class FinalRound extends Round {

    public FinalRound(DbRow data) {
        super(data);
    }
    @Override
    public boolean finish(Event event, Round nextRound) {
        return false;
    }
}
