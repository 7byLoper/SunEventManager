package ru.loper.suneventmanager.manager;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockState;
import java.util.*;
import java.util.logging.Level;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import ru.loper.suneventmanager.SunEventManager;

@RequiredArgsConstructor
public class SchematicManager {
    private final Clipboard currentClipboard;
    private final Set<Material> targetMaterials = new HashSet<>();
    private final Map<Material, List<Location>> materialLocationsMap = new EnumMap<>(Material.class);

    @Getter
    private boolean pasted;

    @Getter
    private boolean scanning;

    @Getter
    private boolean scanComplete;

    @Getter
    private boolean scanFailed;

    private EditSession editSession;
    private World pastedWorld;
    private BukkitTask scanTask;

    public void setSearchMaterials(Material... materials) {
        Collections.addAll(targetMaterials, materials);
        Arrays.stream(materials)
                .forEach(material -> materialLocationsMap.computeIfAbsent(material, ignored -> new ArrayList<>()));
    }

    public void pasteClipboard(Location location, @NotNull Vector eventOffset, boolean ignoreAirBlocks) {
        if (currentClipboard == null || location == null || location.getWorld() == null) {
            SunEventManager.getInstance()
                    .getLogger()
                    .severe("Cannot paste event schematic: clipboard or world location is unavailable.");
            return;
        }

        Location pasteLocation = location.clone().add(eventOffset);
        EditSession session = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(pasteLocation.getWorld()));

        try {
            Operation operation = new ClipboardHolder(currentClipboard)
                    .createPaste(session)
                    .to(BlockVector3.at(pasteLocation.getX(), pasteLocation.getY(), pasteLocation.getZ()))
                    .ignoreAirBlocks(ignoreAirBlocks)
                    .build();
            Operations.complete(operation);
            session.flushSession();

            editSession = session;
            pastedWorld = pasteLocation.getWorld();
            pasted = true;
        } catch (WorldEditException | RuntimeException exception) {
            session.close();
            SunEventManager.getInstance().getLogger().log(Level.SEVERE, "Failed to paste event schematic", exception);
        }
    }

    public void startScanClipboardBlocks(Location location, @NotNull Vector eventOffset, int blocksPerTick) {
        cancelScan();
        clearMaterialLocations();
        scanComplete = false;
        scanFailed = false;

        if (currentClipboard == null || location == null || location.getWorld() == null) {
            scanFailed = true;
            return;
        }

        Location baseLocation =
                location.clone().add(getSchematicOffset(currentClipboard)).add(eventOffset);
        int maxBlocksPerTick = Math.max(256, blocksPerTick);
        scanning = true;

        scanTask = new BukkitRunnable() {
            private final int sizeX = currentClipboard.getDimensions().getX();
            private final int sizeY = currentClipboard.getDimensions().getY();
            private final int sizeZ = currentClipboard.getDimensions().getZ();
            private final BlockVector3 minimum = currentClipboard.getMinimumPoint();
            private int x;
            private int y;
            private int z;

            @Override
            public void run() {
                try {
                    int processed = 0;
                    while (processed++ < maxBlocksPerTick && y < sizeY) {
                        scanBlock(baseLocation, minimum, x, y, z);
                        if (scanFailed) {
                            finishScan(true);
                            cancel();
                            return;
                        }
                        advance();
                    }

                    if (y >= sizeY) {
                        finishScan(false);
                        cancel();
                    }
                } catch (RuntimeException exception) {
                    SunEventManager.getInstance()
                            .getLogger()
                            .log(Level.SEVERE, "Failed to scan event schematic", exception);
                    finishScan(true);
                    cancel();
                }
            }

            private void advance() {
                z++;
                if (z < sizeZ) {
                    return;
                }

                z = 0;
                x++;
                if (x < sizeX) {
                    return;
                }

                x = 0;
                y++;
            }
        }.runTaskTimer(SunEventManager.getInstance(), 1L, 1L);
    }

    private void scanBlock(Location baseLocation, BlockVector3 minimum, int x, int y, int z) {
        BlockVector3 clipboardPos = BlockVector3.at(x + minimum.getX(), y + minimum.getY(), z + minimum.getZ());

        BlockState blockState = currentClipboard.getBlock(clipboardPos);
        Material material = BukkitAdapter.adapt(blockState.getBlockType());

        if (material == Material.BARRIER) {
            handleBarrierBlock(clipboardPos, baseLocation, x, y, z);
            return;
        }

        if (material == Material.BEDROCK) {
            handleBedrockBlock(baseLocation, x, y, z, clipboardPos);
            return;
        }

        if (targetMaterials.contains(material)) {
            materialLocationsMap
                    .computeIfAbsent(material, ignored -> new ArrayList<>())
                    .add(offsetLocation(baseLocation, x, y, z));
        }
    }

    private void handleBedrockBlock(Location baseLocation, int x, int y, int z, BlockVector3 clipboardPos) {
        Location worldLocation = offsetLocation(baseLocation, x, y, z);
        if (targetMaterials.contains(Material.BEDROCK)) {
            materialLocationsMap
                    .computeIfAbsent(Material.BEDROCK, ignored -> new ArrayList<>())
                    .add(worldLocation.clone());
        }

        BlockVector3 lowestSolid = findLowestSolidInClipboard(clipboardPos);
        BlockState fillState;

        if (lowestSolid == null) {
            lowestSolid = BlockVector3.at(
                    clipboardPos.getX(), currentClipboard.getMinimumPoint().getY(), clipboardPos.getZ());
            fillState = BukkitAdapter.adapt(Material.STONE.createBlockData());
        } else {
            fillState = currentClipboard.getBlock(lowestSolid);
        }

        for (int fillY = lowestSolid.getY() + 1; fillY < clipboardPos.getY(); fillY++) {
            BlockVector3 fillPos = BlockVector3.at(clipboardPos.getX(), fillY, clipboardPos.getZ());
            try {
                currentClipboard.setBlock(fillPos, fillState);
            } catch (WorldEditException exception) {
                scanFailed = true;
                SunEventManager.getInstance()
                        .getLogger()
                        .log(Level.SEVERE, "Failed to fill event schematic blocks", exception);
                return;
            }
        }

        Material replaceMaterial = getReplaceMaterial(worldLocation.getBlock().getBiome());
        try {
            currentClipboard.setBlock(clipboardPos, BukkitAdapter.adapt(replaceMaterial.createBlockData()));
        } catch (WorldEditException exception) {
            scanFailed = true;
            SunEventManager.getInstance()
                    .getLogger()
                    .log(Level.SEVERE, "Failed to replace bedrock in event schematic", exception);
        }
    }

    private BlockVector3 findLowestSolidInClipboard(BlockVector3 startPos) {
        for (int y = startPos.getY() - 1;
                y >= currentClipboard.getMinimumPoint().getY();
                y--) {
            BlockVector3 checkPos = BlockVector3.at(startPos.getX(), y, startPos.getZ());
            Material material =
                    BukkitAdapter.adapt(currentClipboard.getBlock(checkPos).getBlockType());
            if (material.isSolid() && material != Material.WATER && material != Material.LAVA) {
                return checkPos;
            }
        }
        return null;
    }

    private void handleBarrierBlock(BlockVector3 clipboardPos, Location baseLocation, int x, int y, int z) {
        Location worldLocation = offsetLocation(baseLocation, x, y, z);
        if (targetMaterials.contains(Material.BARRIER)) {
            materialLocationsMap
                    .computeIfAbsent(Material.BARRIER, ignored -> new ArrayList<>())
                    .add(worldLocation.clone());
        }

        try {
            currentClipboard.setBlock(
                    clipboardPos, BukkitAdapter.adapt(worldLocation.getBlock().getBlockData()));
        } catch (WorldEditException exception) {
            scanFailed = true;
            SunEventManager.getInstance()
                    .getLogger()
                    .log(Level.SEVERE, "Failed to replace barrier block in event schematic", exception);
        }
    }

    public List<Location> getLocationsByMaterial(Material material) {
        return Collections.unmodifiableList(materialLocationsMap.getOrDefault(material, Collections.emptyList()));
    }

    public void clearMaterialLocations() {
        materialLocationsMap.values().forEach(List::clear);
    }

    public void removeSchematic() {
        cancelScan();

        if (editSession != null) {
            try {
                if (pastedWorld != null) {
                    try (EditSession undoSession =
                            WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(pastedWorld))) {
                        editSession.undo(undoSession);
                    }
                }
            } finally {
                editSession.close();
                editSession = null;
                pastedWorld = null;
                pasted = false;
            }
        }

        clearMaterialLocations();
    }

    public void addTargetMaterial(Material material) {
        targetMaterials.add(material);
        materialLocationsMap.computeIfAbsent(material, ignored -> new ArrayList<>());
    }

    private void cancelScan() {
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }
        scanning = false;
    }

    private void finishScan(boolean failed) {
        scanFailed = failed;
        scanComplete = !failed;
        scanning = false;
        scanTask = null;
    }

    private Vector getSchematicOffset(Clipboard clipboard) {
        return new Vector(
                clipboard.getMinimumPoint().getX() - clipboard.getOrigin().getX(),
                clipboard.getMinimumPoint().getY() - clipboard.getOrigin().getY(),
                clipboard.getMinimumPoint().getZ() - clipboard.getOrigin().getZ());
    }

    private Location offsetLocation(Location base, int x, int y, int z) {
        World world = base.getWorld();
        return new Location(world, base.getX() + x, base.getY() + y, base.getZ() + z);
    }

    private Material getReplaceMaterial(Biome biome) {
        String biomeName = biome.name();
        if (biomeName.contains("DESERT")) {
            return Material.SAND;
        }
        if (biomeName.contains("MOUNTAIN") || biomeName.contains("PEAK")) {
            return Material.STONE;
        }
        return Material.GRASS_BLOCK;
    }
}
