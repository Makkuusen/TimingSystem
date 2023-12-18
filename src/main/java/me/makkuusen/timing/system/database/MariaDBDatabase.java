package me.makkuusen.timing.system.database;

import co.aikar.idb.BukkitDB;
import co.aikar.idb.PooledDatabaseOptions;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.TimingSystemConfiguration;

public class MariaDBDatabase extends MySQLDatabase {
    @Override
    public boolean initialize() {
        TimingSystemConfiguration config = TimingSystem.configuration;
        String hostAndPort = config.getSqlHost() + ":" + config.getSqlPort();

        PooledDatabaseOptions options = BukkitDB.getRecommendedOptions(TimingSystem.getPlugin(), config.getSqlUsername(), config.getSqlPassword(), config.getSqlDatabase(), hostAndPort);
        options.getOptions().setDsn("mariadb://" + hostAndPort + "/" + config.getSqlDatabase());

        BukkitDB.createHikariDatabase(TimingSystem.getPlugin(), options);
        return createTables();
    }
}
