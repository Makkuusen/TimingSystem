package me.makkuusen.timing.system.api;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DriverDetails {

    String name;
    String uuid;
    String teamColor;
    boolean offline;
    boolean inpit = false;
    int laps;
    int pits;
    int position;
    int startPosition;
    long gap;
    long gapFromLeader;
    long bestLap;

}
