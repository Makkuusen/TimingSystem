package me.makkuusen.timing.system.text;

import net.kyori.adventure.text.Component;

public enum Success {

    EVENT_SELECTED("Selected new event");

    public String value;
    Success(String value) {
        this.value = value;
    }

    public Component message() {
        return Component.text(value).color(TextUtilities.textSuccess);
    }
}
