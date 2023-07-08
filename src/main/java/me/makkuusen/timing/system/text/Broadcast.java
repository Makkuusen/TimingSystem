package me.makkuusen.timing.system.text;

public enum Broadcast implements MessageLevel{
    CLICK_TO_SIGN,
    CLICK_TO_RESERVE;

    @Override
    public String getKey() {
        return "error." + this.name().toLowerCase();
    }
}
