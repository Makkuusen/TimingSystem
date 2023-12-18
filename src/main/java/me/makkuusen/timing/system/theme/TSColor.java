package me.makkuusen.timing.system.theme;

public enum TSColor {
    PRIMARY,
    SECONDARY,
    ERROR,
    WARNING,
    SUCCESS,
    BROADCAST,
    AWARD,
    TITLE,
    BUTTON,
    BUTTON_ADD,
    BUTTON_REMOVE,
    AWARD_SECONDARY;


    public String getKey() {
        return "color." + this.name().toLowerCase();
    }
}
