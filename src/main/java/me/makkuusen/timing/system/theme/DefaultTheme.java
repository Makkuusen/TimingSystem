package me.makkuusen.timing.system.theme;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

@Getter
@Setter
public class DefaultTheme implements Theme {

    public TextColor primary = TextColor.color(NamedTextColor.GRAY); //7bf200 //gray
    public TextColor secondary = TextColor.color(NamedTextColor.WHITE);
    public TextColor award = TextColor.color(NamedTextColor.GOLD); //#ce00ce //gold
    public TextColor awardSecondary = TextColor.color(NamedTextColor.YELLOW); //#ff80ff //yellow
    public TextColor error = TextColor.color(NamedTextColor.RED); //#ff7a75 //red
    public TextColor warning = TextColor.color(NamedTextColor.YELLOW);
    public TextColor success = TextColor.color(NamedTextColor.GREEN); //7bf200 //green
    public TextColor broadcast = TextColor.color(NamedTextColor.AQUA); //#ff80ff //Aqua
    public TextColor title = TextColor.color(NamedTextColor.DARK_GRAY);

    public DefaultTheme() {}
}
