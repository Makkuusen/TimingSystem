package me.makkuusen.timing.system.api.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class EventResult {

    private String name;
    private long date;
    private String trackName;
    private Integer trackId;
    private int participants;

    private List<RoundResult> rounds;
}
