package me.makkuusen.timing.system.api.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class EventResult {

    private String name;
    private Long date;
    private String trackName;
    private String state;
    private Integer trackId;
    private Integer participants;

    private List<RoundResult> rounds;
}
