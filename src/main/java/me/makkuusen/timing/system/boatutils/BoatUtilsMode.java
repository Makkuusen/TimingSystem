package me.makkuusen.timing.system.boatutils;

public enum BoatUtilsMode {
    VANILLA(-1, 0),
    BROKEN_SLIME_RALLY(0, 0),
    BROKEN_SLIME_RALLY_BLUE(1, 0),
    BROKEN_SLIME_BA_NOFD(2, 0),
    BROKEN_SLIME_PARKOUR(3, 0),
    BROKEN_SLIME_BA_BLUE_NOFD(4,2),
    BROKEN_SLIME_PARKOUR_BLUE(5, 4),
    BROKEN_SLIME_BA(6, 4),
    BROKEN_SLIME_BA_BLUE(7, 4),
    RALLY(8, 5),
    RALLY_BLUE(9, 5),
    BA_NOFD(10, 5),
    PARKOUR(11, 5),
    BA_BLUE_NOFD(12, 5),
    PARKOUR_BLUE(13, 5),
    BA(14, 5),
    BA_BLUE(15, 5);

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
