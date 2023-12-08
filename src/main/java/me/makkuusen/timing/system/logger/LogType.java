package me.makkuusen.timing.system.logger;

import lombok.Getter;

@Getter
public enum LogType {
    TRACK(0),
    EVENT(1);

    private final int id;
    LogType(int id) {
        this.id = id;
    }

    public static LogType of(int id) {
        for(LogType type : values()) {
            if(type.id == id)
                return type;
        }
        return null;
    }
}
