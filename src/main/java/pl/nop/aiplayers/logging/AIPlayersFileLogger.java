package pl.nop.aiplayers.logging;

import pl.nop.aiplayers.AIPlayersPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AIPlayersFileLogger {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final AIPlayersPlugin plugin;
    private final Path logsDirectory;
    private LocalDate currentDate;
    private Path currentLogFile;

    public AIPlayersFileLogger(AIPlayersPlugin plugin) {
        this.plugin = plugin;
        this.logsDirectory = plugin.getDataFolder().toPath().resolve("logs");
    }

    public synchronized void info(String message) {
        write("INFO", message);
    }

    public synchronized void warn(String message) {
        write("WARN", message);
    }

    public synchronized void error(String message) {
        write("ERROR", message);
    }

    private void write(String level, String message) {
        refreshLogFile();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String line = "[" + timestamp + "] [" + level + "] " + message + System.lineSeparator();
        try {
            Files.createDirectories(logsDirectory);
            Files.writeString(currentLogFile, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write to AIPlayers log file: " + e.getMessage());
        }
    }

    private void refreshLogFile() {
        LocalDate now = LocalDate.now();
        if (currentDate == null || !currentDate.equals(now)) {
            currentDate = now;
            String filename = DATE_FORMAT.format(now) + "_log.txt";
            currentLogFile = logsDirectory.resolve(filename);
        }
    }

    public Path getCurrentLogFile() {
        refreshLogFile();
        return currentLogFile;
    }
}
