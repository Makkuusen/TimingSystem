package me.makkuusen.timing.system.text;

public enum Warning implements MessageLevel{
    DRIVERS_LEFT_OUT,
    NO_LONGER_SPECTATING,
    NO_LONGER_SIGNED,
    NO_LONGER_RESERVE,
    PLAYER_NO_LONGER_SIGNED,
    PLAYER_NO_LONGER_RESERVE,
    DANGEROUS_COMMAND;

    @Override
    public String getKey() {
        return "warning." + this.name().toLowerCase();
    }
}
