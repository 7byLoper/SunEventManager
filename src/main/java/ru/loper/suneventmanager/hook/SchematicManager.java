package ru.loper.suneventmanager.hook;

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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import ru.loper.suneventmanager.SunEventManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class SchematicManager {
    private final Clipboard currentClipboard;
    private final Set<Material> targetMaterials = new HashSet<>();
    private final Map<Material, List<Location>> materialLocationsMap = new ConcurrentHashMap<>();
    @Getter
    private boolean pasted = false;
    private EditSession editSession;

    private static @NotNull Material getReplaceMaterial(Location worldLoc) {
        Biome biome = worldLoc.getBlock().getBiome();

        if (biome.name().contains("DESERT")) {
            return Material.SAND;
        } else if (biome.name().contains("MOUNTAIN") || biome.name().contains("PEAK")) {
            return Material.STONE;
        } else {
            return Material.GRASS_BLOCK;
        }
    }

    public void setSearchMaterials(Material... materials) {
        Collections.addAll(targetMaterials, materials);
        Arrays.stream(materials).forEach(m -> materialLocationsMap.put(m, new ArrayList<>()));
    }

    public void pasteClipboard(Location location, @NotNull Vector eventOffset) {
        location = location.clone().add(eventOffset);

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()))) {
            Operation operation = new ClipboardHolder(currentClipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                    .build();
            Operations.complete(operation);

            this.editSession = editSession;
            pasted = true;
        } catch (WorldEditException e) {
            throw new RuntimeException("Failed to paste schematic", e);
        } catch (NullPointerException e) {
            SunEventManager.getInstance().getLogger().warning("Failed to paste schematic (schematic file is null)");
        }
    }

    public void scanClipboardBlocks(Location location, @NotNull Vector eventOffset) {
        Location baseLocation = location.clone().add(getSchematicOffset(currentClipboard)).add(eventOffset);

        for (int y = 0; y < currentClipboard.getDimensions().getY(); y++) {
            for (int x = 0; x < currentClipboard.getDimensions().getX(); x++) {
                for (int z = 0; z < currentClipboard.getDimensions().getZ(); z++) {
                    BlockVector3 clipboardPos = BlockVector3.at(
                            x + currentClipboard.getMinimumPoint().getX(),
                            y + currentClipboard.getMinimumPoint().getY(),
                            z + currentClipboard.getMinimumPoint().getZ()
                    );

                    BlockState blockState = currentClipboard.getBlock(clipboardPos);
                    Material material = BukkitAdapter.adapt(blockState.getBlockType());

                    if (material == Material.BARRIER) {
                        handleBarrierBlock(clipboardPos, baseLocation, x, y, z);
                        continue;
                    }

                    if (material == Material.BEDROCK) {
                        handleBedrockBlock(baseLocation, x, y, z, clipboardPos);
                        continue;
                    }

                    if (targetMaterials.contains(material)) {
                        Location blockLocation = baseLocation.clone().add(new Vector(x, y, z));
                        materialLocationsMap.get(material).add(blockLocation);
                    }
                }
            }
        }
    }

    private void handleBedrockBlock(Location baseLocation, int x, int y, int z, BlockVector3 clipboardPos) {
        Location worldLoc = baseLocation.clone().add(new Vector(x, y, z));

        BlockVector3 lowestSolid = findLowestSolidInClipboard(clipboardPos);
        BlockState blockState;
        if (lowestSolid == null) {
            lowestSolid = BlockVector3.at(x, 0, z);
            blockState = BukkitAdapter.adapt(Material.STONE.createBlockData());
        } else {
            blockState = currentClipboard.getBlock(lowestSolid);
        }


        for (int fillY = lowestSolid.getY() + 1; fillY < clipboardPos.getY(); fillY++) {
            BlockVector3 fillPos = BlockVector3.at(clipboardPos.getX(), fillY, clipboardPos.getZ());
            try {
                currentClipboard.setBlock(fillPos, blockState);
            } catch (WorldEditException e) {
                throw new RuntimeException("Failed to fill blocks in clipboard", e);
            }
        }

        Material replaceMaterial = getReplaceMaterial(worldLoc);
        try {
            currentClipboard.setBlock(clipboardPos, BukkitAdapter.adapt(replaceMaterial.createBlockData()));
        } catch (WorldEditException e) {
            throw new RuntimeException("Failed to replace bedrock in clipboard", e);
        }
    }

    private BlockVector3 findLowestSolidInClipboard(BlockVector3 startPos) {
        for (int y = startPos.getY() - 1; y >= currentClipboard.getMinimumPoint().getY(); y--) {
            BlockVector3 checkPos = BlockVector3.at(startPos.getX(), y, startPos.getZ());
            Material material = BukkitAdapter.adapt(currentClipboard.getBlock(checkPos).getBlockType());
            if (material.isSolid() && material != Material.AIR && material != Material.WATER && material != Material.LAVA) {
                return checkPos;
            }
        }
        return null;
    }

    private void handleBarrierBlock(BlockVector3 clipboardPos,
                                    Location baseLocation, int x, int y, int z) {
        try {
            Block worldBlock = baseLocation.clone().add(new Vector(x, y, z)).getBlock();
            currentClipboard.setBlock(clipboardPos, BukkitAdapter.adapt(worldBlock.getBlockData()));
        } catch (WorldEditException e) {
            throw new RuntimeException("Failed to handle barrier block", e);
        }
    }

    public List<Location> getLocationsByMaterial(Material material) {
        return materialLocationsMap.getOrDefault(material, Collections.emptyList());
    }

    public void clearMaterialLocations() {
        materialLocationsMap.values().forEach(List::clear);
    }

    public void removeSchematic() {
        if (editSession != null) {
            editSession.undo(editSession);
            editSession.close();
            editSession = null;
        }
        clearMaterialLocations();
    }

    private Vector getSchematicOffset(Clipboard clipboard) {
        return new Vector(
                clipboard.getMinimumPoint().getX() - clipboard.getOrigin().getX(),
                clipboard.getMinimumPoint().getY() - clipboard.getOrigin().getY(),
                clipboard.getMinimumPoint().getZ() - clipboard.getOrigin().getZ()
        );
    }

    public void addTargetMaterial(Material material) {
        targetMaterials.add(material);
        materialLocationsMap.putIfAbsent(material, new ArrayList<>());
    }
}