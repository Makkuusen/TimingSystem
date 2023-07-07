package me.makkuusen.timing.system.text;


import net.kyori.adventure.text.Component;

public enum Errors {

    NO_EVENT_SELECTED("You have no event selected."),
    PLAYER_NOT_FOUND("Could not find player."),
    PERMISSION_DENIED("Permission denied.");

    public final String value;

    Errors(String value) {
        this.value = value;
    }

    public Component message() {
        return Component.text(value).color(TextUtilities.textError);
    }
}