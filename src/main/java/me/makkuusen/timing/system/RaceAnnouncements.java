package me.makkuusen.timing.system;

public class RaceAnnouncements {

    private static void sendAnnouncement (Race race, String key, String... replacements) {

        for (RaceParticipant rp : race.getRaceParticipants())
        {
            if (rp.getPlayer() != null)
            {
                TimingSystem.getPlugin().sendMessage(rp.getPlayer(), key, replacements);
            }
        }
    }

    public static void sendPit (RaceDriver raceDriver, int pit)
    {
        sendAnnouncement(raceDriver.race, "messages.announcements.pitstop", "%player%", raceDriver.getTSPlayer().getName(), "%pit%", String.valueOf(pit));
    }

    public static void broadcastFinish (RaceDriver raceDriver, long time)
    {
        sendAnnouncement(raceDriver.race, "messages.announcements.finish", "%player%", raceDriver.getTSPlayer().getName(), "%position%", String.valueOf(raceDriver.getPosition()), "%time%", ApiUtilities.formatAsTime(time));
    }
}
