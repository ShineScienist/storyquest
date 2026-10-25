package com.shinecreator.storyquest.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.shinecreator.storyquest.StoryQuest;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Files for JSON quest exchange and automatic DAT backups.
 */
public final class QuestStorage {
    public static final int MAX_BACKUPS = 20;
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private QuestStorage() {}

    public static Path questsDir() {
        return FMLPaths.CONFIGDIR.get().resolve(StoryQuest.MODID).resolve("quests");
    }

    public static Path backupsDir() {
        return FMLPaths.CONFIGDIR.get().resolve(StoryQuest.MODID).resolve("backups");
    }

    public static String defaultExportName() {
        return "quests-" + LocalDateTime.now().format(STAMP);
    }

    public static Path resolveJson(String name) {
        String fileName = name == null ? "" : name.trim();
        if (fileName.isEmpty()) {
            throw new IllegalArgumentException("File name is empty.");
        }
        if (!fileName.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("Use only letters, digits, '_' and '-' in the file name.");
        }
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".json")) {
            fileName += ".json";
        }
        return questsDir().resolve(fileName);
    }

    public static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    public static void write(Path path, String text) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, text, StandardCharsets.UTF_8);
    }

    /**
     * Copies the world quest DAT before a destructive operation.
     *
     * @return the backup path, or {@code null} if the DAT does not exist yet
     */
    public static Path backup(MinecraftServer server) throws IOException {
        Path source = server.getWorldPath(LevelResource.ROOT)
                .resolve("data")
                .resolve("storyquest_quests.dat");
        if (!Files.exists(source)) {
            return null;
        }

        Path dir = backupsDir();
        Files.createDirectories(dir);
        String base = "storyquest_quests-" + LocalDateTime.now().format(STAMP);
        Path target = dir.resolve(base + ".dat");
        int suffix = 1;
        while (Files.exists(target)) {
            target = dir.resolve(base + "-" + suffix++ + ".dat");
        }
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        prune();
        return target;
    }

    private static void prune() throws IOException {
        List<Path> files = new ArrayList<>();
        try (var stream = Files.list(backupsDir())) {
            stream.filter(path -> path.getFileName().toString().endsWith(".dat"))
                    .forEach(files::add);
        }
        files.sort(Comparator.comparingLong(path -> path.toFile().lastModified()));
        while (files.size() > MAX_BACKUPS) {
            Files.deleteIfExists(files.remove(0));
        }
    }
}
