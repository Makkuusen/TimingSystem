package me.makkuusen.timing.system.theme;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;


public class BoatLabsTheme extends Theme {

    private TextColor primary = TextColor.fromHexString("#7bf200");
    private TextColor secondary = TextColor.color(NamedTextColor.WHITE);
    private TextColor award = TextColor.fromHexString("#ff80ff");
    private TextColor awardSecondary = TextColor.fromHexString("#ffffff");
    private TextColor error = TextColor.color(NamedTextColor.RED);
    private TextColor warning = TextColor.color(NamedTextColor.YELLOW);
    private TextColor success = TextColor.fromHexString("#7bf200");
    private TextColor broadcast = TextColor.fromHexString("#ff80ff");
    public TextColor title = TextColor.color(NamedTextColor.DARK_GRAY);
    public TextColor button = TextColor.fromHexString("#d91eff");
    public TextColor buttonRemove = TextColor.color(NamedTextColor.RED);
    public TextColor buttonAdd = TextColor.color(NamedTextColor.YELLOW);

    public BoatLabsTheme() {
        setPrimary(primary);
        setSecondary(secondary);
        setAward(award);
        setAwardSecondary(awardSecondary);
        setError(error);
        setWarning(warning);
        setSuccess(success);
        setBroadcast(broadcast);
        setTitle(title);
        setButton(button);
        setButtonRemove(buttonRemove);
        setButtonAdd(buttonAdd);
    }
}
