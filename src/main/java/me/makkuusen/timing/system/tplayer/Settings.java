package me.makkuusen.timing.system.tplayer;

import co.aikar.idb.DbRow;
import lombok.Getter;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.database.EventDatabase;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Boat;

import java.awt.*;
import java.util.UUID;

public class Settings {

    private final UUID uuid;
    @Getter
    private Boat.Type boat;
    @Getter
    private boolean chestBoat;
    private boolean toggleSound;
    private String color;
    @Getter
    private boolean verbose;
    @Getter
    private boolean timeTrial;
    @Getter
    private boolean override;
    @Getter
    private boolean compactScoreboard;
    @Getter
    private boolean sendFinalLaps;
    public Settings(UUID uuid, DbRow data) {
        this.uuid = uuid;
        boat = stringToType(data.getString("boat"));
        chestBoat = data.get("chestBoat") instanceof Boolean ? data.get("chestBoat") : data.get("chestBoat").equals(1);
        toggleSound = data.get("toggleSound") instanceof Boolean ? data.get("toggleSound") : data.get("toggleSound").equals(1);
        verbose = data.get("verbose") instanceof Boolean ? data.get("verbose") : data.get("verbose").equals(1);
        timeTrial = data.get("timetrial") instanceof  Boolean ? data.get("timetrial") : data.get("timetrial").equals(1);
        color = data.getString("color");
        compactScoreboard = data.get("compactScoreboard") instanceof Boolean ? data.get("compactScoreboard") : data.get("compactScoreboard").equals(1);
        sendFinalLaps = data.get("sendFinalLaps") instanceof Boolean ? data.get("sendFinalLaps") : data.get("sendFinalLaps").equals(1);
    }

    public String getHexColor() {
        return color;
    }

    public void setHexColor(String hexColor) {
        color = hexColor;
        EventDatabase.getDriverFromRunningHeat(uuid).ifPresent(driver -> driver.getHeat().updateScoreboard());
        TimingSystem.getDatabase().playerUpdateValue(uuid, "color", hexColor);
    }

    public org.bukkit.Color getBukkitColor() {
        var c = Color.decode(color);
        return org.bukkit.Color.fromRGB(c.getRed(), c.getGreen(), c.getBlue());
    }

    public TextColor getTextColor() {
        return TextColor.fromHexString(color);
    }

    public void setBoat(Boat.Type boat) {
        this.boat = boat;
        TimingSystem.getDatabase().playerUpdateValue(uuid, "boat", boat.name());
    }

    public void setChestBoat(boolean b) {
        if (chestBoat == b)
            return;
        chestBoat = b;
        TimingSystem.getDatabase().playerUpdateValue(uuid, "chestBoat", chestBoat);
    }

    public void toggleOverride() {
        override = !override;
        TimingSystem.getDatabase().playerUpdateValue(uuid, "override", override);
    }

    public void toggleVerbose() {
        verbose = !verbose;
        TimingSystem.getDatabase().playerUpdateValue(uuid, "verbose", verbose);
    }

    public void toggleTimeTrial() {
        timeTrial = !timeTrial;
        TimingSystem.getDatabase().playerUpdateValue(uuid, "timetrial", timeTrial);
    }

    public void toggleSound() {
        toggleSound = !toggleSound;
        TimingSystem.getDatabase().playerUpdateValue(uuid, "toggleSound", toggleSound);
    }

    public void toggleCompactScoreboard() {
        this.compactScoreboard = !compactScoreboard;
        TimingSystem.getDatabase().playerUpdateValue(uuid, "compactScoreboard", compactScoreboard);
    }

    public void toggleSendFinalLaps() {
        sendFinalLaps = !sendFinalLaps;
        TimingSystem.getDatabase().playerUpdateValue(uuid, "sendFinalLaps", sendFinalLaps);
    }

    public boolean isCompactScoreboard() {
        return compactScoreboard;
    }

    public boolean getCompactScoreboard() {
        return compactScoreboard;
    }

    public boolean isSound() {
        return toggleSound;
    }

    public Material getBoatMaterial() {
        String boat = getBoat().name();
        if (chestBoat) {
            boat += "_CHEST";
        }
        if (boat.contains("BAMBOO")) {
            boat += "_RAFT";
        } else {
            boat += "_BOAT";
        }
        return Material.valueOf(boat);
    }

    private Boat.Type stringToType(String boatType) {
        if (boatType == null) {
            return Boat.Type.BIRCH;
        }
        try {
            return Boat.Type.valueOf(boatType);
        } catch (IllegalArgumentException e) {
            //REDWOOD is the only old option possible.
            return Boat.Type.SPRUCE;
        }
    }
}
