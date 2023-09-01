package me.makkuusen.timing.system.boatutils;

public enum BoatUtilsMode {
    VANILLA(-1, 0),
    RALLY( 0, 0),
    RALLY_BLUE(1, 0),
    BA(2, 0),
    PARKOUR(3, 0),
    BA_BLUE(4,2);

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
