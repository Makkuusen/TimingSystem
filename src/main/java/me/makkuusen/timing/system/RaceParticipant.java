package me.makkuusen.timing.system;

import org.bukkit.entity.Player;

public abstract class RaceParticipant {

    private TSPlayer tsPlayer;
    protected Race race;

    public RaceParticipant(TSPlayer tsPlayer, Race race){
        this.tsPlayer = tsPlayer;
        this.race = race;
    }

    public TSPlayer getTSPlayer() {
        return tsPlayer;
    }

    public Player getPlayer() { return tsPlayer.getPlayer();}

    public void setTSPlayer(TSPlayer tsPlayer) {
        this.tsPlayer = tsPlayer;
    }
}
