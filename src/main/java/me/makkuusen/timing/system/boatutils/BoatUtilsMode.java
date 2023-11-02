package me.makkuusen.timing.system.boatutils;

public enum BoatUtilsMode {
    VANILLA(-1, 0),
    RALLY(0, 0),
    RALLY_BLUE(1, 0),
    BA_NOFD(2, 0),
    PARKOUR(3, 0),
    BA_BLUE_NOFD(4,2),
    PARKOUR_BLUE(5, 4),
    BA(6, 4),
    BA_BLUE(7, 4);

    private final short id;
    private final short version;

    BoatUtilsMode(int id, int version) {
        this.id = (short) id;
        this.version = (short) version;
    }

    public short getId(){
        return id;
    }

    public short getRequiredVersion() {
        return version;
    }
}
