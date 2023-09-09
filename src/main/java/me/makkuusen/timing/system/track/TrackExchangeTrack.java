package me.makkuusen.timing.system.track;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.math.BlockVector2;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.boatutils.BoatUtilsMode;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class TrackExchangeTrack {
    public static final String CURRENT_VERSION = "1.9";
    public static final String PATH = TimingSystem.getPlugin().getDataFolder() + File.separator + "tracks" + File.separator;

    private String version;
    private String name;
    private String ownerUUID;
    private Long dateCreated;
    private String guiItem;
    private String spawnLocation;
    private String trackType;
    private String trackMode;
    private String boatUtilsMode;
    private Integer weight;
    private String options;
    private Long totalTimeSpent;
    private List<TrackRegion> trackRegions = new ArrayList<>();
    private List<TrackLocation> trackLocations = new ArrayList<>();
    private Clipboard clipboard;
    private Track track;

    private Vector clipboardOffset;

    /**
     * For new file from track.
     */
    public TrackExchangeTrack(@NotNull Track track, @Nullable Clipboard clipboard) {
        version = CURRENT_VERSION;
        name = track.getDisplayName();
        ownerUUID = track.getOwner().getUniqueId().toString();
        dateCreated = track.getDateCreated();
        guiItem = ApiUtilities.itemToString(track.getGuiItem());
        spawnLocation = ApiUtilities.locationToString(track.getSpawnLocation());
        trackType = track.getType().toString();
        trackMode = track.getMode().toString();
        boatUtilsMode = track.getBoatUtilsMode().toString();
        weight = track.getWeight();
        options = Arrays.toString(track.getOptions());
        totalTimeSpent = track.getTotalTimeSpent();
        trackRegions.addAll(track.getRegions());
        trackLocations.addAll(track.getTrackLocations());
        this.clipboard = clipboard;

        this.track = track;
    }

    /**
     * For new track from imported file.
     */
    public TrackExchangeTrack() {
        version = null;
        name = null;
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

    /**
     * For new track from imported file with newnames.
     */
    public TrackExchangeTrack(String newTrackName) {
        version = null;
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

    public TrackExchangeTrack writeToFile(Location origin) throws IOException {
        origin = origin.toBlockLocation();

        String simpleName = name.replace(" ", "").toLowerCase();
        File pack = new File(PATH + simpleName);
        File packZip = new File(pack.toPath() + ".zip");
        File trackDataFile = new File(PATH + simpleName + File.separator + "0.trackdata");
        File trackSchematicFile = new File(PATH + simpleName + File.separator + "1.trackschem");
        if(!packZip.exists()) {
            pack.mkdir(); // create track specific folder, throws exception if this track already exists.
        } else throw new FileAlreadyExistsException(pack.getPath());

        // trackDataFile
        try(FileWriter writer = new FileWriter(trackDataFile)) {
            JSONObject main = new JSONObject();
            main.put("version", version);
            main.put("name", name);
            main.put("ownerUUID", ownerUUID);
            main.put("dateCreated", dateCreated);
            main.put("guiItem", guiItem);
            main.put("spawnLocation", spawnLocation);
            main.put("trackType", trackType);
            main.put("trackMode", trackMode);
            main.put("boatUtilsMode", boatUtilsMode);
            main.put("weight", weight);
            main.put("options", options);
            main.put("totalTimeSpent", totalTimeSpent);

            main.put("from", ApiUtilities.locationToString(origin));

            JSONObject clipOffset = new JSONObject();
            if(clipboard != null) {
                Vector offsetVec = getOffset(ApiUtilities.getLocationFromBlockVector3(origin.getWorld(), clipboard.getOrigin()), origin);
                clipOffset.put("x", offsetVec.getX());
                clipOffset.put("y", offsetVec.getY());
                clipOffset.put("z", offsetVec.getZ());
            } else {
                clipOffset.put("x", 0);
                clipOffset.put("y", 0);
                clipOffset.put("z", 0);
            }
            main.put("clipOffset", clipOffset);

            JSONObject trackRegions = new JSONObject();
            int regionId = 0;
            for(TrackRegion region : this.trackRegions) {
                JSONObject r = new JSONObject();
                r.put("index", region.getRegionIndex());
                r.put("regionType", region.getRegionType().toString());
                r.put("regionShape", region.getShape().toString());
                r.put("spawnLocation", ApiUtilities.locationToString(region.getSpawnLocation()));
                r.put("minP", ApiUtilities.locationToString(region.getMinP()));
                r.put("maxP", ApiUtilities.locationToString(region.getMaxP()));

                if(region instanceof TrackPolyRegion polyRegion) {
                    JSONObject vecs = new JSONObject();
                    int pointsId = 0;
                    for(BlockVector2 vec2 : polyRegion.getPolygonal2DRegion().getPoints()) {
                        JSONObject vec = new JSONObject();
                        vec.put("x", vec2.getX());
                        vec.put("z", vec2.getZ());
                        pointsId++;
                        vecs.put(String.valueOf(pointsId), vec);
                    }
                    r.put("points", vecs);
                }
                regionId++;
                trackRegions.put(String.valueOf(regionId), r);
            }
            main.put("trackRegions", trackRegions);

            JSONObject trackLocations = new JSONObject();
            int locationId = 0;
            for(TrackLocation location : this.trackLocations) {
                JSONObject l = new JSONObject();
                l.put("index", location.getIndex());
                l.put("location", ApiUtilities.locationToString(location.getLocation()));
                l.put("locationType", location.getLocationType().toString());
                locationId++;
                trackLocations.put(String.valueOf(locationId), l);
            }
            main.put("trackLocations", trackLocations);
            writer.write(main.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
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

        return this;
    }

    public TrackExchangeTrack readFile(Player player, String trackName) throws IOException {
        unzipDir(new File(PATH + trackName + ".zip"), new File(PATH));
        File trackDataFile = new File(PATH + "0.trackdata");
        File trackSchematicFile = new File(PATH + "1.trackschem");

        try(Reader reader = new FileReader(trackDataFile)) {
            JSONObject main = (JSONObject) new JSONParser().parse(reader);
            version = (String) main.get("version");
            if(name == null) name = (String) main.get("name");
            ownerUUID = (String) main.get("ownerUUID");
            dateCreated = (Long) main.get("dateCreated");
            guiItem = (String) main.get("guiItem");
            spawnLocation = (String) main.get("spawnLocation");
            trackType = (String) main.get("trackType");
            trackMode = (String) main.get("trackMode");
            boatUtilsMode = (String) main.get("boatUtilsMode");
            weight = Integer.parseInt(String.valueOf(main.get("weight")));
            options = (String) main.get("options");
            totalTimeSpent = (Long) main.get("totalTimeSpent");

            if (!TrackDatabase.trackNameAvailable(name)) {
                Text.send(player, Error.TRACK_EXISTS);
                trackDataFile.delete();
                trackSchematicFile.delete();
                return null;
            }

            Location from = ApiUtilities.stringToLocation((String) main.get("from"));
            Vector offset = getOffset(from, player.getLocation().toBlockLocation());
            Location newSpawnLocation = getNewLocation(player.getWorld(), ApiUtilities.stringToLocation(spawnLocation), offset);

            JSONObject clipOff = (JSONObject) main.get("clipOffset");
            clipboardOffset = new Vector(Double.parseDouble(String.valueOf(clipOff.get("x"))), Double.parseDouble(String.valueOf(clipOff.get("y"))), Double.parseDouble(String.valueOf(clipOff.get("z"))));

            this.track = TrackDatabase.trackNewFromTrackExchange(name, UUID.fromString(ownerUUID), dateCreated, newSpawnLocation, Track.TrackType.valueOf(trackType), Track.TrackMode.valueOf(trackMode), ApiUtilities.stringToItem(guiItem), weight, (JSONObject) main.get("trackRegions"), (JSONObject) main.get("trackLocations"), BoatUtilsMode.valueOf(boatUtilsMode), offset);
        } catch (IOException | ParseException e) {
            Text.send(player, Error.GENERIC);
            e.printStackTrace();
            return null;
        }

        try(ClipboardReader clipboardReader = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getReader(new FileInputStream(trackSchematicFile))) {
            clipboard = clipboardReader.read();
        } catch (FileNotFoundException e) {
            clipboard = null;
        }

        trackDataFile.delete();
        trackSchematicFile.delete();

        return this;
    }

    public String getVersion() {
        return version;
    }

    public Clipboard getClipboard() {
        return clipboard;
    }

    public Vector getClipboardOffset() {
        return clipboardOffset;
    }

    public Track getTrack() {
        return track;
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
}
