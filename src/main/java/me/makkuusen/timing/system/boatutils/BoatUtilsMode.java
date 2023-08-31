package me.makkuusen.timing.system.boatutils;

public enum BoatUtilsMode {
    VANILLA(-1),
    RALLY( 0),
    RALLY_BLUE(1),
    BA(2),
    PARKOUR(3);

    private final short id;

    BoatUtilsMode(int id) {
        this.id = (short) id;
    }

    public short getId(){
        return id;
    }
}
