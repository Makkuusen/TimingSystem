package me.makkuusen.timing.system;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SimpleScoreboard {

    private Scoreboard scoreboard;

    private String title;
    private Map<String, Integer> scores;
    private List<Team> teams;

    public SimpleScoreboard(String title)
    {
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.title = fixFormating(title);
        this.scores = Maps.newLinkedHashMap();
        this.teams = Lists.newArrayList();
    }

    public void add(String text)
    {
        add(text, null);
    }

    public void add(String text, Integer score)
    {
        text = fixFormating(text);

        Preconditions.checkArgument(text.length() < 48, "text cannot be over 48 characters in length");
        text = fixDuplicates(text);
        scores.put(text, score);
    }

    private String fixFormating(String s)
    {
        int limit = 16;

        if (s.length() > limit)
        {
            String format = "";

            for (int i = 0; i < limit; i++)
            {
                char c = s.charAt(i);

                if (c == '§')
                {
                    i++;
                    char n = s.charAt(i);

                    if (n == 'r') { format = "§r"; }
                    else { format += "§" + n; }
                }
            }

            return s.substring(0, limit) + format + s.substring(limit);
        }

        else
        {
            return s;
        }
    }
    private String fixDuplicates(String text)
    {
        while (scores.containsKey(text))
            text += "§r";
        if (text.length() > 48)
            text = text.substring(0, 47);
        return text;
    }

    private Map.Entry<Team, String> createTeam(String text)
    {
        String result = "";
        if (text.length() <= 16)
            return new AbstractMap.SimpleEntry<>(null, text);
        Team team = scoreboard.registerNewTeam("text-" + scoreboard.getTeams().size());
        Iterator<String> iterator = Splitter.fixedLength(16).split(text).iterator();
        team.setPrefix(iterator.next());
        result = iterator.next();
        if (text.length() > 32)
            team.setSuffix(iterator.next());
        teams.add(team);
        return new AbstractMap.SimpleEntry<>(team, result);
    }

    @SuppressWarnings("deprecation")
    public void build()
    {
        Objective obj = scoreboard.registerNewObjective((title.length() > 16 ? title.substring(0, 15) : title), "dummy");
        obj.setDisplayName(title);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int index = scores.size();

        for (Map.Entry<String, Integer> text : scores.entrySet())
        {
            Map.Entry<Team, String> team = createTeam(text.getKey());
            Integer score = text.getValue() != null ? text.getValue() : index;
            OfflinePlayer player = new Message(team.getValue());
            if (team.getKey() != null)
                team.getKey().addPlayer(player);
            obj.getScore(player).setScore(score);
            index -= 1;
        }
    }

    public void reset()
    {
        title = null;
        scores.clear();
        for (Team t : teams)
            t.unregister();
        teams.clear();
    }

    public Scoreboard getScoreboard()
    {
        return scoreboard;
    }

    public void send(Player... players)
    {
        for (Player p : players)
            p.setScoreboard(scoreboard);
    }

    public static void resetScoreboard(Player p)
    {
        SimpleScoreboard scoreboard = new SimpleScoreboard("");
        scoreboard.build();
        scoreboard.send(p);
        scoreboard.reset();
    }

}

class Message implements OfflinePlayer
{
    String name;

    public Message(String name)
    {
        this.name = name;
    }

    /**
     * Returns the name of this player
     *
     * @return Player name
     */
    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Map<String, Object> serialize()
    {
        return null;
    }

    @Override
    public UUID getUniqueId()
    {
        return UUID.randomUUID();
    }

    @Override
    public @NotNull PlayerProfile getPlayerProfile()
    {
        return null;
    }

    @Override
    public boolean isOp()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setOp(boolean value)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public Location getBedSpawnLocation()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getLastLogin()
    {
        return 0;
    }

    @Override
    public long getLastSeen()
    {
        return 0;
    }

    @Override
    public void incrementStatistic(Statistic statistic) throws IllegalArgumentException { }

    @Override
    public void decrementStatistic(Statistic statistic) throws IllegalArgumentException { }

    @Override
    public void incrementStatistic(Statistic statistic, int i) throws IllegalArgumentException { }

    @Override
    public void decrementStatistic(Statistic statistic, int i) throws IllegalArgumentException { }

    @Override
    public void setStatistic(Statistic statistic, int i) throws IllegalArgumentException { }

    @Override
    public int getStatistic(Statistic statistic) throws IllegalArgumentException {
        return 0;
    }

    @Override
    public void incrementStatistic(Statistic statistic, Material material) throws IllegalArgumentException { }

    @Override
    public void decrementStatistic(Statistic statistic, Material material) throws IllegalArgumentException { }

    @Override
    public int getStatistic(Statistic statistic, Material material) throws IllegalArgumentException {
        return 0;
    }

    @Override
    public void incrementStatistic(Statistic statistic, Material material, int i) throws IllegalArgumentException { }

    @Override
    public void decrementStatistic(Statistic statistic, Material material, int i) throws IllegalArgumentException { }

    @Override
    public void setStatistic(Statistic statistic, Material material, int i) throws IllegalArgumentException { }

    @Override
    public void incrementStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException { }

    @Override
    public void decrementStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException { }

    @Override
    public int getStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {
        return 0;
    }

    @Override
    public void incrementStatistic(Statistic statistic, EntityType entityType, int i) throws IllegalArgumentException { }

    @Override
    public void decrementStatistic(Statistic statistic, EntityType entityType, int i) { }

    @Override
    public void setStatistic(Statistic statistic, EntityType entityType, int i) { }

    public @Nullable Location getLastDeathLocation()
    {
        return null;
    }

    @Override
    public long getFirstPlayed()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getLastPlayed()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Player getPlayer()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasPlayedBefore()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isBanned()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isOnline()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isWhitelisted()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setWhitelisted(boolean value)
    {
        // TODO Auto-generated method stub

    }
}
