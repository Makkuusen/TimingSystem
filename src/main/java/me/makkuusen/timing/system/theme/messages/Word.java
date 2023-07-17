package me.makkuusen.timing.system.theme.messages;

public enum Word implements MessageNoColor {
    OPEN, CLOSED, FINISH, LOAD, START, RESET;

    @Override
    public String getKey() {
        return "word." + this.name().toLowerCase();
    }

}
