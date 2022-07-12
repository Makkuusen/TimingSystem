package me.makkuusen.timing.system.participant;

import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.TPlayer;

public abstract class Participant {

    @Getter
    @Setter
    private TPlayer tPlayer;

    public Participant(TPlayer tPlayer) {
        this.tPlayer = tPlayer;
    }

}
