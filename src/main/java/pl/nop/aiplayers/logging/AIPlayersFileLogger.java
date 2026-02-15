package pl.nop.aiplayers.logging;

import pl.nop.aiplayers.AIPlayersPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;

public class AIPlayersFileLogger {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final AIPlayersPlugin plugin;
    private final Path logsDirectory;
    private Path currentLogFile;
    private final ElasticBufferBridge elasticBufferBridge;

    public AIPlayersFileLogger(AIPlayersPlugin plugin) {
        this.plugin = plugin;
        this.logsDirectory = plugin.getDataFolder().toPath().resolve("logs");
        this.elasticBufferBridge = ElasticBufferBridge.create(plugin);
        refreshLogFile();
    }

    public synchronized void info(String message) {
        write("INFO", message, Collections.emptyMap());
    }

    public synchronized void info(String message, Map<String, String> columns) {
        write("INFO", message, columns);
    }

    public synchronized void warn(String message) {
        write("WARN", message, Collections.emptyMap());
    }

    public synchronized void warn(String message, Map<String, String> columns) {
        write("WARN", message, columns);
    }

    public synchronized void error(String message) {
        write("ERROR", message, Collections.emptyMap());
    }

    public synchronized void error(String message, Map<String, String> columns) {
        write("ERROR", message, columns);
    }

    private void write(String level, String message, Map<String, String> columns) {
        refreshLogFile();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String line = "[" + timestamp + "] [" + level + "] " + message + formatColumns(columns) + System.lineSeparator();
        try {
            Files.createDirectories(logsDirectory);
            Files.writeString(currentLogFile, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write to AIPlayers log file: " + e.getMessage());
        }
        elasticBufferBridge.log(level, message, columns);
    }

    private String formatColumns(Map<String, String> columns) {
        if (columns == null || columns.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(" | columns=");
        boolean first = true;
        for (Map.Entry<String, String> entry : columns.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue());
            first = false;
        }
        return builder.toString();
    }

    private void refreshLogFile() {
        if (currentLogFile == null) {
            String filename = DATE_FORMAT.format(LocalDateTime.now()) + "_log.txt";
            currentLogFile = logsDirectory.resolve(filename);
        }
    }

    public Path getCurrentLogFile() {
        refreshLogFile();
        return currentLogFile;
    }
}
