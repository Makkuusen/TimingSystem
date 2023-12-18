package me.makkuusen.timing.system.participant;

import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.database.TSDatabase;

import java.util.UUID;

public class Subscriber extends Participant {

    public Subscriber(TPlayer tPlayer) {
        super(tPlayer);
    }

    public Subscriber(DbRow data) {
        super(TSDatabase.getPlayer(UUID.fromString(data.getString("uuid"))));
    }

    public boolean equals(Object o) {

        if (o instanceof Subscriber subscriber) {
            return getTPlayer().getUniqueId().equals(subscriber.getTPlayer().getUniqueId());
        } else if (o instanceof TPlayer tPlayer) {
            return getTPlayer().getUniqueId().equals(tPlayer.getUniqueId());
        } else if (o instanceof UUID uuid) {
            return getTPlayer().getUniqueId().equals(uuid);
        }

        return false;
    }

    public enum Type {
        SUBSCRIBER, RESERVE
    }

}
