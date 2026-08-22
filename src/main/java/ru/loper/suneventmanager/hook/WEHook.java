package ru.loper.suneventmanager.hook;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import lombok.experimental.UtilityClass;
import org.bukkit.plugin.Plugin;

@UtilityClass
public class WEHook {
    public Clipboard loadClipboard(String schematicName, Plugin plugin) {
        if (schematicName == null || schematicName.isEmpty()) {
            plugin.getLogger().warning("Schematic filename %s not specified".formatted(schematicName));
            return null;
        }

        File schematicsFolder = new File(plugin.getDataFolder(), "schematics");
        File file;
        try {
            file = new File(schematicsFolder, schematicName).getCanonicalFile();
            File canonicalFolder = schematicsFolder.getCanonicalFile();
            if (!file.toPath().startsWith(canonicalFolder.toPath())) {
                plugin.getLogger().warning("Schematic path escapes schematics directory: " + schematicName);
                return null;
            }
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to resolve schematic path: " + schematicName, exception);
            return null;
        }

        if (!file.isFile()) {
            plugin.getLogger().warning("Schematic file not found: " + file.getAbsolutePath());
            return null;
        }

        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            plugin.getLogger().warning("Unknown schematic format for file: " + file.getName());
            return null;
        }

        try (FileInputStream fis = new FileInputStream(file);
                ClipboardReader reader = format.getReader(fis)) {
            return reader.read();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load schematic: " + file.getName(), e);
            return null;
        }
    }
}
