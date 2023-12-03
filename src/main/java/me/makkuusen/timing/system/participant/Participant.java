package me.makkuusen.timing.system.participant;

import co.aikar.idb.DbRow;
import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.database.Database;
import me.makkuusen.timing.system.TPlayer;

import java.util.UUID;

@Getter
public abstract class Participant {

    @Setter
    private TPlayer tPlayer;

    public Participant(TPlayer tPlayer) {
        this.tPlayer = tPlayer;
    }

    public Participant(DbRow data) {
        this.tPlayer = Database.getPlayer(UUID.fromString(data.getString("uuid")));
    }

}
