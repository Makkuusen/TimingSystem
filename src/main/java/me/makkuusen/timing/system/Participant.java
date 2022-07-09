package me.makkuusen.timing.system;

import lombok.Getter;
import lombok.Setter;

public abstract class Participant {

    @Getter
    @Setter
    private TPlayer tPlayer;

    public Participant(TPlayer tPlayer) {
        this.tPlayer = tPlayer;
    }

}
