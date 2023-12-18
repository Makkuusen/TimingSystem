package me.makkuusen.timing.system.heat;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.theme.Theme;
import me.makkuusen.timing.system.tplayer.TPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class ScoreboardUtils {
    public static Component getDriverLine(Driver driver, int pos, boolean compact, Theme theme) {
        return Component.text().append(paddPos(pos, driver)).append((compact ? Component.empty() : getDivider(theme))).append(Component.text("           ")).append(getTeamIcon(driver)).append(paddName(driver, compact)).build();
    }

    public static Component getDriverLineQualyTime(long laptime, Driver driver, int pos, boolean compact, Theme theme) {
        return Component.text().append(paddPos(pos, driver)).append((compact ? Component.empty() : getDivider(theme))).append(Component.text(" ")).append(paddTime(ApiUtilities.formatAsTime(laptime)).color(getSecondaryColor(theme))).append(getTeamIcon(driver)).append(paddName(driver, compact)).build();
    }

    public static Component getDriverLineQualyGap(long timeDiff, Driver driver, int pos, boolean compact, Theme theme) {
        return Component.text().append(paddPos(pos, driver)).append((compact ? Component.empty() : getDivider(theme))).append(Component.text(" +").color(NamedTextColor.GREEN)).append(paddGap(ApiUtilities.formatAsQualificationGap(timeDiff)).color(NamedTextColor.GREEN)).append(getTeamIcon(driver)).append(paddName(driver, compact)).build();
    }

    public static Component getDriverLineNegativeQualyGap(long timeDiff, Driver driver, int pos, boolean compact, Theme theme) {
        return Component.text().append(paddPos(pos, driver)).append((compact ? Component.empty() : getDivider(theme))).append(Component.text(" -").color(NamedTextColor.RED)).append(paddGap(ApiUtilities.formatAsQualificationGap(timeDiff)).color(NamedTextColor.RED)).append(getTeamIcon(driver)).append(paddName(driver, compact)).build();
    }

    public static Component getDriverLineRace(Driver driver, int pos, boolean compact, Theme theme) {
        return Component.text().append(paddPos(pos, driver)).append((compact ? Component.empty() : getDivider(theme))).append(Component.text("           ")).append(getTeamIcon(driver)).append(paddName(driver, compact)).append(getPits(compact, theme)).append(Component.text(0).color(TextColor.color(0xFFFFFF))).build();
    }

    public static Component getDriverLineRace(Driver driver, int pits, int pos, boolean compact, Theme theme) {
        return Component.text().append(paddPos(pos, driver)).append((compact ? Component.empty() : getDivider(theme))).append(Component.text("           ")).append(getTeamIcon(driver)).append(paddName(driver, compact)).append(getPits(compact, theme)).append(Component.text(pits).color(getPitColour(driver, pits))).build();
    }

    public static Component getDriverLineRaceInPit(Driver driver, int pits, int pos, boolean compact, Theme theme) {
        return Component.text().append(paddPos(pos, driver)).append((compact ? Component.empty() : getDivider(theme))).append(Component.text(" In Pit   ", NamedTextColor.GRAY)).append(getTeamIcon(driver)).append(paddName(driver, compact)).append(getPits(compact, theme)).append(Component.text(pits).color(getPitColour(driver, pits))).build();
    }

    public static Component getDriverLineRaceOffline(Driver driver, int pits, int pos, boolean compact, Theme theme) {
        return Component.text().append(paddPos(pos, driver)).append((compact ? Component.empty() : getDivider(theme))).append(Component.text(" Offline  ", NamedTextColor.GRAY)).append(getTeamIcon(driver)).append(paddName(driver, compact)).append(getPits(compact, theme)).append(Component.text(pits).color(getPitColour(driver, pits))).build();
    }

    public static Component getDriverLineRaceLaps(int laps, Driver driver, int pits, int pos, boolean compact, Theme theme) {
        return Component.text().append(paddPos(pos, driver)).append((compact ? Component.empty() : getDivider(theme))).append(Component.text(" Lap: ")).append(paddLaps(laps).color(getSecondaryColor(theme))).append(Component.text(" ")).append(getTeamIcon(driver)).append(paddName(driver, compact)).append(getPits(compact, theme)).append(Component.text(pits).color(getPitColour(driver, pits))).build();
    }

    public static Component getDriverLineRaceGap(long gap, Driver driver, int pits, int pos, boolean compact, Theme theme) {
        return Component.text().append(paddPos(pos, driver)).append((compact ? Component.empty() : getDivider(theme))).append(Component.text(" +").color(NamedTextColor.GREEN)).append(paddGap(ApiUtilities.formatAsRacingGap(gap)).color(NamedTextColor.GREEN)).append(getTeamIcon(driver)).append(paddName(driver, compact)).append(getPits(compact, theme)).append(Component.text(pits).color(getPitColour(driver, pits))).build();
    }

    public static Component getDriverLineNegativeRaceGap(long gap, Driver driver, int pits, int pos, boolean compact, Theme theme) {
        return Component.text().append(paddPos(pos, driver)).append((compact ? Component.empty() : getDivider(theme))).append(Component.text(" -").color(NamedTextColor.RED)).append(paddGap(ApiUtilities.formatAsRacingGap(gap)).color(NamedTextColor.RED)).append(getTeamIcon(driver)).append(paddName(driver, compact)).append(getPits(compact, theme)).append(Component.text(pits).color(getPitColour(driver, pits))).build();
    }

    private static Component getDivider(Theme theme) {
        return Component.text("|").color(getPrimaryColor(theme));
    }

    public static Component paddName(Driver driver, boolean compact) {
        return paddName(driver.getTPlayer(), compact);
    }

    public static Component paddName(TPlayer tPlayer, boolean compact) {

        TextComponent.Builder c = Component.text().content("");
        if (compact) {
            var shortName = tPlayer.getSettings().getShortName();
            c.append(Component.text(shortName));
            int spaces = 4 - shortName.length();
            c.append(Component.text(" ".repeat(Math.max(0, spaces))));
        } else {
            c.append(Component.text(tPlayer.getName()));
            int spaces = 16 - tPlayer.getName().length();
            c.append(Component.text(" ".repeat(Math.max(0, spaces))));
        }
        return c.build();
    }

    public static Component paddPos(int pos, Driver driver) {
        TextColor posColour;

        switch(pos) {
            case 1 -> posColour = TextColor.color(0xF6F31A);
            case 2 -> posColour = TextColor.color(0xC3C3C3);
            case 3 -> posColour = TextColor.color(0xCD7F32);
            default -> posColour = TextColor.color(0xFFFFFF);
        }

        TextComponent.Builder b = Component.text().content(String.valueOf(pos)).color(posColour);
        boolean hasFastestLap = driver.getHeat().getFastestLapUUID() == driver.getTPlayer().getUniqueId();

        if(hasFastestLap) b.decorate(TextDecoration.UNDERLINED);
        if(driver.isFinished()) b.decorate(TextDecoration.ITALIC);

        if(!(pos > 9)) {
            b.append(Component.text(" ").decoration(TextDecoration.UNDERLINED, false).decoration(TextDecoration.ITALIC, false));
        }

        return b.build();
    }

    public static Component paddGap(String gap) {
        if (gap.length() == 5) {
            return Component.text(gap + "  ");
        }
        return Component.text(gap);
    }

    public static Component paddTime(String time) {
        if (time.length() == 6) {
            return Component.text(time + "  ");
        }
        return Component.text(time);
    }

    public static Component paddLaps(int laps) {
        if (laps < 10) {
            return Component.text("0" + laps);
        }
        return Component.text(laps);
    }

    private static TextColor getPitColour(Driver driver, int pits) {
        TextColor color = NamedTextColor.RED;
        if (pits >= driver.getHeat().getTotalPits()) color = NamedTextColor.GREEN;
        else if (pits > 0) color = NamedTextColor.GOLD;
        return color;
    }

    public static TextColor getSecondaryColor(Theme theme) {
        return theme.getSecondary();
    }

    public static TextColor getPrimaryColor(Theme theme) {
        return TextColor.color(theme.getPrimary().value());
    }

    private static Component getTeamIcon(Driver driver) {
        return Component.text("§l§o||§r ").color(driver.getTPlayer().getSettings().getTextColor());
    }

    private static Component getPits(boolean compact, Theme theme) {
        if (compact) {
            return Component.text(" ");
        }
        return Component.text("Pits: ").color(getPrimaryColor(theme));
    }

}
