package me.makkuusen.timing.system.theme;

import net.kyori.adventure.text.format.TextColor;
 public interface Theme {
        TextColor getPrimary();

        void setPrimary(TextColor color);
        TextColor getSecondary();
        void setSecondary(TextColor color);
        TextColor getAward();
        void setAward(TextColor color);
        TextColor getAwardSecondary();
        void setAwardSecondary(TextColor color);
        TextColor getError();
        void setError(TextColor color);
        TextColor getWarning();
        void setWarning(TextColor color);
        TextColor getSuccess();
        void setSuccess(TextColor color);
        TextColor getBroadcast();
        void setBroadcast(TextColor color);

        TextColor getTitle();
        void setTitle(TextColor color);

}
