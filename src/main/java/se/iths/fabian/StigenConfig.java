package se.iths.fabian;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class StigenConfig {

    private final Properties properties;
    private final Path configFilePath;
    private static final String DEFAULT_DIRECTORY_KEY = "default_directory";

    public StigenConfig() {
        String userHome = System.getProperty("user.home");
        Path configDir = Paths.get(userHome, ".stigen");
        this.configFilePath = configDir.resolve("config.properties");
        this.properties = new Properties();

        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            if (Files.exists(configFilePath)) {
                try (InputStream input = Files.newInputStream(configFilePath)) {
                    properties.load(input);
                }
            }
        } catch (IOException e) {
            // Handle exceptions related to file access, e.g., log an error
            // For now, we'll proceed with default properties
            System.err.println("Warning: Could not load or create config file: " + e.getMessage());
        }
    }

    public String getDefaultDirectory() {
        return properties.getProperty(DEFAULT_DIRECTORY_KEY);
    }

    public void setDefaultDirectory(String path) {
        if (path != null) {
            properties.setProperty(DEFAULT_DIRECTORY_KEY, path);
        } else {
            properties.remove(DEFAULT_DIRECTORY_KEY);
        }
    }

    public void save() {
        try (OutputStream output = Files.newOutputStream(configFilePath)) {
            properties.store(output, "Stigen Configuration");
        } catch (IOException e) {
            System.err.println("Warning: Could not save config file: " + e.getMessage());
        }
    }
}
