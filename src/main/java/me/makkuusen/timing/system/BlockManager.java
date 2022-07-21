package me.makkuusen.timing.system;

import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackRegion;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BlockManager {

    private final Track track;
    private final HashMap<Integer, List<Material>> trackRegionBlocks = new HashMap<>();

    public BlockManager(Track track) {
        this.track = track;
    }

    public void setStartingGrid() {
        track.getGridRegions().values().stream().forEach(trackRegion -> {
            getGridBorderBlocks(trackRegion).forEach(block -> block.setType(Material.WHITE_CARPET));
            getGridGroundBlocks(trackRegion, true).forEach(block -> block.setType(Material.WHITE_CONCRETE));
        });
    }

    public void clearStartingGrid() {
        track.getGridRegions().values().stream().forEach(trackRegion -> {
            getGridBorderBlocks(trackRegion).forEach(block -> block.setType(Material.AIR));
            List<Material> mats = trackRegionBlocks.get(trackRegion.getId());
            if (mats != null) {
                int count = 0;
                for (Block block : getGridGroundBlocks(trackRegion, false)) {
                    block.setType(mats.get(count++));
                }
            }
        });
    }

    private List<Block> getGridBorderBlocks(TrackRegion region) {
        List<Block> selected = new ArrayList<>();
        int y = region.getMaxP().getBlockY();
        World world = region.getMaxP().getWorld();

        for (int x1 = region.getMinP().getBlockX() + 1; x1 < region.getMaxP().getBlockX(); x1++) {
            selected.add(world.getBlockAt(x1, y, region.getMinP().getBlockZ()));
            selected.add(world.getBlockAt(x1, y, region.getMaxP().getBlockZ()));
        }

        for (int z1 = region.getMinP().getBlockZ() + 1; z1 < region.getMaxP().getBlockZ(); z1++) {
            selected.add(world.getBlockAt(region.getMinP().getBlockX(), y, z1));
            selected.add(world.getBlockAt(region.getMaxP().getBlockX(), y, z1));
        }

        return selected;
    }

    private List<Block> getGridGroundBlocks(TrackRegion region, boolean saveMaterial) {
        List<Block> selected = new ArrayList<>();
        int y = region.getMaxP().getBlockY() - 1;
        World world = region.getMaxP().getWorld();

        for (int x = region.getMinP().getBlockX() + 1; x < region.getMaxP().getBlockX(); x++) {
            for (int z = region.getMinP().getBlockZ() + 1; z < region.getMaxP().getBlockZ(); z++) {
                selected.add(world.getBlockAt(x, y, z));
            }
        }
        List<Material> mats = new ArrayList<>();
        selected.forEach(block -> mats.add(block.getType()));
        if (saveMaterial) trackRegionBlocks.put(region.getId(), mats);
        return selected;
    }
}
