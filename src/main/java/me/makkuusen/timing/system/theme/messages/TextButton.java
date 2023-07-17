package me.makkuusen.timing.system.theme.messages;

public enum TextButton implements MessageNoColor {
    ADD_HEAT, ADD_ROUND, VIEW_EVENT;

    @Override
    public String getKey() {
        return "text_button." + this.name().toLowerCase();
    }
}
