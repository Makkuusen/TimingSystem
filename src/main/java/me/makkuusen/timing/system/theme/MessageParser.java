package me.makkuusen.timing.system.theme;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.theme.messages.ActionBar;
import me.makkuusen.timing.system.theme.messages.Message;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;

public class MessageParser {

    public static Component getComponentWithColors(String text, Message level, Theme theme) {

        TextColor color = NamedTextColor.WHITE;
        List<TextDecoration> decorations = new ArrayList<>();

        String[] strings = text.split("&");
        Component component = Component.empty();

        boolean first = true;
        for (String string : strings) {
            String message = first ? "" : "&";
            if (!string.isEmpty()) {
                String option = string.substring(0, 1);
                message = string.substring(1);
                switch (option) {
                    case "1" -> color = theme.getPrimary();
                    case "2" -> color = theme.getSecondary();
                    case "s" -> color = theme.getSuccess();
                    case "w" -> color = theme.getWarning();
                    case "e" -> color = theme.getError();
                    case "b" -> color = theme.getBroadcast();
                    case "a" -> color = theme.getAward();
                    case "c" -> color = theme.getAwardSecondary();
                    case "t" -> color = theme.getTitle();
                    case "o" -> decorations.add(TextDecoration.ITALIC);
                    case "l" -> decorations.add(TextDecoration.BOLD);
                    case "r" -> {
                        decorations = new ArrayList<>();
                        color = NamedTextColor.WHITE;
                    }
                    default -> message = first ? string : "&" + string;
                }
            }
            first = false;
            if (level instanceof ActionBar) {
                if (!color.asHexString().equalsIgnoreCase("#ffffff")) {
                    color = TextColor.fromHexString(ApiUtilities.darkenHexColor(color.asHexString(), 0.1));
                }
            }
            component = component.append(buildComponent(message, color, decorations));
        }

        return component;

    }

    public static Component buildComponent(String message, TextColor color, List<TextDecoration> decorations) {
        var newComponent = Component.text(message).color(color);
        for (TextDecoration decoration : decorations) {
            newComponent = newComponent.decorate(decoration);
        }
        return newComponent;
    }

}
