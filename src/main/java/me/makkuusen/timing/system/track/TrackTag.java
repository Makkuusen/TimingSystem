package me.makkuusen.timing.system.track;

import lombok.Getter;

@Getter
public class TrackTag {
    String value;

    public TrackTag(String value){
        this.value = value.toUpperCase();
    }

    @Override
    public boolean equals(Object o){
        if (o instanceof TrackTag trackTag) {
            return trackTag.getValue().equalsIgnoreCase(value);
        }

        if (o instanceof String tackTagString) {
            return tackTagString.equalsIgnoreCase(value);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
