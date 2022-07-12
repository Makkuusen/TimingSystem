package me.makkuusen.timing.system.race;

import me.makkuusen.timing.system.TPlayer;
import org.bukkit.entity.Player;

public abstract class RaceParticipant {

    private TPlayer tPlayer;
    protected Race race;

    public RaceParticipant(TPlayer tPlayer, Race race){
        this.tPlayer = tPlayer;
        this.race = race;
    }

    public TPlayer getTSPlayer() {
        return tPlayer;
    }

    public Player getPlayer() { return tPlayer.getPlayer();}

    public void setTSPlayer(TPlayer tPlayer) {
        this.tPlayer = tPlayer;
    }
}
