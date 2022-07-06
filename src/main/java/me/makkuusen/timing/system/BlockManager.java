package me.makkuusen.timing.system;

import com.sk89q.worldedit.regions.Region;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public class BlockManager {

    private Track track;

    public BlockManager(Track track) {
        this.track = track;
    }

    public void setStartingGridBarriers(){
        track.getGridRegions().values().stream().forEach( trackRegion -> getGridBlocks(trackRegion).forEach(block -> block.setType(Material.WHITE_CARPET)));
    }

    public void clearStartingGridBarriers()
    {
        track.getGridRegions().values().stream().forEach( trackRegion -> getGridBlocks(trackRegion).forEach(block -> block.setType(Material.AIR)));
    }

    private List<Block> getGridBlocks(TrackRegion region){
        List<Block> selected = new ArrayList<>();
        int y = region.getMaxP().getBlockY();
        World world = region.getMaxP().getWorld();

        for (int x1 = region.getMinP().getBlockX() + 1; x1 < region.getMaxP().getBlockX(); x1++)
        {
            selected.add(world.getBlockAt(x1, y, region.getMinP().getBlockZ()));
            selected.add(world.getBlockAt(x1, y, region.getMaxP().getBlockZ()));
        }

        for (int z1 = region.getMinP().getBlockZ() + 1; z1 < region.getMaxP().getBlockZ(); z1++)
        {
            selected.add(world.getBlockAt(region.getMinP().getBlockX(), y, z1));
            selected.add(world.getBlockAt(region.getMaxP().getBlockX(), y, z1));
        }

        return selected;
    }
}
