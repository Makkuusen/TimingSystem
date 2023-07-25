package me.makkuusen.timing.system.theme.messages;

public enum Hover implements MessageNoColor {
    CLICK_TO_VIEW,
    CLICK_TO_DELETE,
    CLICK_TO_EDIT,
    CLICK_TO_EDIT_POSITION,
    CLICK_TO_ADD,
    CLICK_TO_OPEN,
    CLICK_TO_CLOSE,
    CLICK_TO_FINISH,
    CLICK_TO_SELECT,
    CLICK_TO_LOAD,
    CLICK_TO_RESET,
    CLICK_TO_START;

    @Override
    public String getKey() {
        return "hover." + this.name().toLowerCase();
    }
}
