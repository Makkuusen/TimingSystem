package me.makkuusen.timing.system.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public class TextButtons {

    public static TextColor buttonColor = TextColor.color(NamedTextColor.YELLOW);
    public static TextColor buttonRemoveColor = TextColor.color(NamedTextColor.RED);
    public static TextColor buttonAddColor = TextColor.color(NamedTextColor.GREEN);

    // [»] [≈] [+] [-] [√] [?] [≡] [±]

    public static Component getViewButton() {
        return Component.text("[»]").color(buttonColor).hoverEvent(getClickToViewHoverEvent());
    }

    public static Component getEditButton() {
        return Component.text("[Edit]").color(buttonColor).hoverEvent(getClickToEditHoverEvent());
    }

    public static Component getEditButton(String value) {
        return TextUtilities.getBrackets(value).hoverEvent(getClickToEditHoverEvent());
    }

    public static Component getAddButton() {
        return Component.text("[+]").color(buttonAddColor).hoverEvent(HoverEvent.showText(Component.text("Click to add")));
    }

    public static Component getRemoveButton() {
        return Component.text("[-]").color(buttonRemoveColor).hoverEvent(HoverEvent.showText(Component.text("Click to delete")));
    }

    public static Component getRefreshButton() {
        return Component.text("↻").color(buttonColor).hoverEvent(HoverEvent.showText(Component.text("Refresh")));
    }

    public static Component getMoveButton() {
        return Component.text("[±]").color(buttonColor);
    }

    public static Component getAddButton(String extra) {
        return Component.text("[Add " + extra + "]").color(buttonAddColor);
    }

    public static HoverEvent<Component> getClickToViewHoverEvent() {
        return HoverEvent.showText(Component.text("Click to view"));
    }

    public static HoverEvent<Component> getClickToEditHoverEvent() {
        return HoverEvent.showText(Component.text("Click to edit"));
    }

    public static HoverEvent<Component> getClickToAddHoverEvent() {
        return HoverEvent.showText(Component.text("Click to add"));
    }

    public static Component getFooterButtons(String eventName) {
        return TextUtilities.getSpacersStart()
                .append(Component.text("[Event]").color(buttonColor).clickEvent(ClickEvent.runCommand("/event info " + eventName)).hoverEvent(getClickToViewHoverEvent()))
                .append(TextUtilities.getSpacersEnd());
    }
}
