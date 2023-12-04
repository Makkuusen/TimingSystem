package me.makkuusen.timing.system.track;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.boatutils.BoatUtilsMode;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.Theme;
import me.makkuusen.timing.system.theme.messages.Error;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Getter
public class TrackExchangeTrack implements Serializable {
    public static final String PATH = TimingSystem.getPlugin().getDataFolder() + File.separator + "tracks" + File.separator;
    @Serial
    private static final long serialVersionUID = 1683242551882882102L;

    private String name;
    private String ownerUUID;
    private Long dateCreated;
    private String guiItem;
    private String spawnLocation;
    private String trackType;
    private String trackMode;
    private Integer boatUtilsMode;
    private Integer weight;
    private String options;
    private Long totalTimeSpent;
    private List<SerializableRegion> trackRegions = new ArrayList<>();
    private List<SerializableLocation> trackLocations = new ArrayList<>();
    private String origin;
    private String clipboardOffset;

    private transient Clipboard clipboard;
    private transient Track track;

    /**
     * For new file from track.
     */
    public TrackExchangeTrack(@NotNull Track track, @Nullable Clipboard clipboard) {
        name = track.getDisplayName();
        ownerUUID = track.getOwner().getUniqueId().toString();
        dateCreated = track.getDateCreated();
        guiItem = ApiUtilities.itemToString(track.getGuiItem());
        spawnLocation = ApiUtilities.locationToString(track.getSpawnLocation());
        trackType = track.getType().toString();
        trackMode = track.getMode().toString();
        boatUtilsMode = (int) track.getBoatUtilsMode().getId();
        weight = track.getWeight();
        options = Arrays.toString(track.getOptions());
        totalTimeSpent = track.getTotalTimeSpent();
        trackRegions.addAll(track.getRegions().stream().map(SerializableRegion::new).toList());
        trackLocations.addAll(track.getTrackLocations().stream().map(SerializableLocation::new).toList());
        this.clipboard = clipboard;

        this.track = track;
    }

    /**
     * For new track from imported file.
     */
    public TrackExchangeTrack() {
        this(null);
    }

    /**
     * For new track from imported file with newnames.
     */
    public TrackExchangeTrack(String newTrackName) {
        name = newTrackName;
        ownerUUID = null;
        dateCreated = null;
        guiItem = null;
        spawnLocation = null;
        trackType = null;
        trackMode = null;
        boatUtilsMode = null;
        weight = null;
        options = null;
        totalTimeSpent = null;
    }

    public static Clipboard makeSchematicFile(Player player) {
        try {
            Region r = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player)).getSelection();
            BlockArrayClipboard clipboard = new BlockArrayClipboard(r);
            Operations.complete(new ForwardExtentCopy(BukkitAdapter.adapt(player.getWorld()), r, clipboard, r.getMinimumPoint()));
            return clipboard;
        } catch (WorldEditException e) {
            Text.send(player, Error.GENERIC);
            return null;
        }
    }

    public void pasteTrackSchematic(Player player) {
        try(EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(player.getWorld()))) {
            if (clipboard != null) {
                Operations.complete(new ClipboardHolder(clipboard).createPaste(editSession).to(BlockVector3.at(player.getLocation().x() + getClipboardOffset().getX(), player.getLocation().y() + getClipboardOffset().getY(), player.getLocation().z() + getClipboardOffset().getZ())).copyEntities(true).build());
            } else player.sendMessage(Component.text("Loading without schematic").color(Theme.getTheme(player).getWarning()));
        } catch (WorldEditException e) {
            Text.send(player, Error.GENERIC);
        }
    }

    public void writeTrackToFile(Location origin) throws IOException {
        this.origin = ApiUtilities.locationToString(origin.toBlockLocation());

        String simpleName = name.replace(" ", "").toLowerCase();
        File pack = new File(PATH + simpleName);
        File packZip = new File(pack.toPath() + ".zip");
        File trackDataFile = new File(PATH + simpleName + File.separator + "0.trackdata");
        File trackSchematicFile = new File(PATH + simpleName + File.separator + "1.trackschem");
        if(!packZip.exists()) {
            pack.mkdir(); // create track specific folder, throws exception if this track already exists.
        } else
            throw new FileAlreadyExistsException(pack.getPath());

        if(clipboard != null)
            clipboardOffset = getOffset(ApiUtilities.getLocationFromBlockVector3(origin.getWorld(), clipboard.getOrigin()), origin).toString();


        try(FileOutputStream fileOut = new FileOutputStream(trackDataFile)) {
            try(ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
                out.writeObject(this);
            }
        }

        // trackSchematicFile
        if(clipboard != null) {
            try(ClipboardWriter clipboardWriter = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(trackSchematicFile))) {
                clipboardWriter.write(clipboard);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        zipDir(pack);
        trackDataFile.delete();
        trackSchematicFile.delete();
        pack.delete();
    }

    public static TrackExchangeTrack readFile(Player player, String oldName, String trackName) throws IOException {
        unzipDir(new File(PATH + oldName + ".zip"), new File(PATH));
        File trackDataFile = new File(PATH + "0.trackdata");
        File trackSchematicFile = new File(PATH + "1.trackschem");

        TrackExchangeTrack trackExchangeTrack;
        try(FileInputStream fileIn = new FileInputStream(trackDataFile)) {
            try(ObjectInputStream in = new ObjectInputStream(fileIn)) {
                trackExchangeTrack = (TrackExchangeTrack) in.readObject();
                trackExchangeTrack.name = trackName;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if(!TrackDatabase.trackNameAvailable(trackExchangeTrack.name)) {
            Text.send(player, Error.TRACK_EXISTS);
            trackDataFile.delete();
            trackSchematicFile.delete();
            return null;
        }

        Vector offset = getOffset(ApiUtilities.stringToLocation(trackExchangeTrack.origin).toBlockLocation(), player.getLocation().toBlockLocation());
        Location newSpawnLocation = getNewLocation(player.getWorld(), ApiUtilities.stringToLocation(trackExchangeTrack.spawnLocation), offset);

        trackExchangeTrack.track = TrackDatabase.trackNewFromTrackExchange(trackExchangeTrack, newSpawnLocation, offset, player);

        try(ClipboardReader clipboardReader = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getReader(new FileInputStream(trackSchematicFile))) {
            trackExchangeTrack.clipboard = clipboardReader.read();
        } catch (FileNotFoundException e) {
            trackExchangeTrack.clipboard = null;
        }

        trackDataFile.delete();
        trackSchematicFile.delete();

        return trackExchangeTrack;
    }

    public Location getSpawnLocation(World world) {
        var location = ApiUtilities.stringToLocation(spawnLocation);
        return new Location(world, location.x(), location.y(), location.z());
    }

    public UUID getOwnerUUID() {
        return UUID.fromString(ownerUUID);
    }

    public Track.TrackType getTrackType() {
        return Track.TrackType.valueOf(trackType);
    }

    public Track.TrackMode getTrackMode() {
        return Track.TrackMode.valueOf(trackMode);
    }

    public ItemStack getGuiItem() {
        return ApiUtilities.stringToItem(guiItem);
    }

    public BoatUtilsMode getBoatUtilsMode() {
        return BoatUtilsMode.getMode(boatUtilsMode);
    }

    public Vector getClipboardOffset() {
        String[] split = clipboardOffset.split(",");
        float x = Float.parseFloat(split[0]);
        float y = Float.parseFloat(split[1]);
        float z = Float.parseFloat(split[2]);
        return new Vector(x, y, z);
    }

    public static Vector getOffset(Location from, Location to) {
        var vector = new Vector();
        vector.setX(from.getX() - to.getX());
        vector.setY(from.getY() - to.getY());
        vector.setZ(from.getZ() - to.getZ());
        return vector;
    }

    public static Location getNewLocation(World newWorld, Location oldLocation, Vector offset) {
        return new Location(newWorld, oldLocation.getX(), oldLocation.getY(), oldLocation.getZ(), oldLocation.getYaw(), oldLocation.getPitch()).subtract(offset);
    }

    private static void zipDir(File dir) throws IOException {
        try(ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(dir.getPath() + ".zip"))) {
            Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    if(attributes.isSymbolicLink()) return FileVisitResult.CONTINUE;

                    try(FileInputStream fileIn = new FileInputStream(file.toFile())) {
                        Path targetFile = dir.toPath().relativize(file);
                        zipOut.putNextEntry(new ZipEntry(targetFile.toString()));

                        byte[] buffer = new byte[1024];
                        int len;
                        while((len = fileIn.read(buffer)) > 0) {
                            zipOut.write(buffer, 0, len);
                        }
                        zipOut.closeEntry();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException e) {
                    TimingSystem.getPlugin().logger.log(Level.SEVERE, "Unable to zip file " + file.toString());
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private static void unzipDir(File dir, File dest) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(dir.getPath()))) {
            ZipEntry zipEntry = zipIn.getNextEntry();
            while(zipEntry != null) {
                Path newPath = dest.toPath().resolve(zipEntry.getName()).normalize();
                Files.copy(zipIn, newPath, StandardCopyOption.REPLACE_EXISTING);
                zipIn.closeEntry();
                zipEntry = zipIn.getNextEntry();
            }
        }
    }

    @AllArgsConstructor
    @Getter
    public static class SerializableRegion implements Serializable {
        private final int id;
        private final int trackId;
        private final int regionIndex;
        private final String regionType;
        private final String regionShape;
        private final String spawnLocation;
        private final String minP;
        private final String maxP;
        private final List<String> points;

        public SerializableRegion(TrackRegion region) {
            id = region.getId();
            trackId = region.getTrackId();
            regionIndex = region.getRegionIndex();
            regionType = region.getRegionType().name();
            regionShape = region.getShape().name();
            spawnLocation = ApiUtilities.locationToString(region.getSpawnLocation());
            minP = ApiUtilities.locationToString(region.getMinP());
            maxP = ApiUtilities.locationToString(region.getMaxP());
            if(region instanceof TrackPolyRegion polyRegion)
                points = polyRegion.getPolygonal2DRegion().getPoints().stream().map(vector -> vector.getX() + " " + vector.getZ()).toList();
            else
                points = new ArrayList<>();
        }

        public TrackRegion.RegionType getRegionType() {
            return TrackRegion.RegionType.valueOf(regionType);
        }

        public TrackRegion.RegionShape getRegionShape() {
            return TrackRegion.RegionShape.valueOf(regionShape);
        }

        public Location getSpawnLocation(World world) {
            var location = ApiUtilities.stringToLocation(spawnLocation);
            return new Location(world, location.x(), location.y(), location.z());
        }

        public Location getMinP(World world) {
            var location = ApiUtilities.stringToLocation(minP);
            return new Location(world, location.x(), location.y(), location.z());
        }

        public Location getMaxP(World world) {
            var location = ApiUtilities.stringToLocation(maxP);
            return new Location(world, location.x(), location.y(), location.z());
        }

        public List<BlockVector2> getPoints() {
            return points.stream().map(s -> {
                String[] vectorString = s.split(" ");
                return BlockVector2.at(Integer.parseInt(vectorString[0]), Integer.parseInt(vectorString[1]));
            }).toList();
        }
    }

    @AllArgsConstructor
    @Getter
    public static class SerializableLocation implements Serializable {
        private final int trackId;
        private final int regionIndex;
        private final String location;
        private final String type;

        public SerializableLocation(TrackLocation region) {
            trackId = region.getTrackId();
            regionIndex = region.getIndex();
            location = ApiUtilities.locationToString(region.getLocation());
            type = region.getLocationType().toString();
        }

        public Location getLocation(World world) {
            var location = ApiUtilities.stringToLocation(this.location);
            return new Location(world, location.x(), location.y(), location.z());
        }

        public TrackLocation.Type getType() {
            return TrackLocation.Type.valueOf(type);
        }

        public TrackLocation toTrackLocation(World world) {
            return new TrackLocation(trackId, regionIndex, getLocation(world), getType());
        }
    }
}
