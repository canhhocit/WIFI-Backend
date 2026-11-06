package com.wificaptive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class WifiCaptivePortalApplication {

    static {
        loadDotEnv();
    }

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(WifiCaptivePortalApplication.class, args);
    }

    /**
     * Automatically loads .env file into Java System properties if present,
     * ensuring Spring Boot ${ENV_VAR} placeholders resolve during local development
     * without overriding active environment variables in OS / Docker containers.
     */
    public static void loadDotEnv() {
        Path[] candidatePaths = new Path[]{
                Paths.get(".env"),
                Paths.get("wifi-captive-backend/.env"),
                Paths.get("../.env")
        };

        for (Path candidate : candidatePaths) {
            if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                try {
                    List<String> lines = Files.readAllLines(candidate, StandardCharsets.UTF_8);
                    int loadedCount = 0;
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                            continue;
                        }
                        int separatorIndex = trimmed.indexOf('=');
                        if (separatorIndex > 0) {
                            String key = trimmed.substring(0, separatorIndex).trim();
                            String value = trimmed.substring(separatorIndex + 1).trim();

                            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                                    (value.startsWith("'") && value.endsWith("'"))) {
                                value = value.substring(1, value.length() - 1);
                            }

                            // Do not override system env or already set system properties
                            if (System.getProperty(key) == null && System.getenv(key) == null) {
                                System.setProperty(key, value);
                                loadedCount++;
                            }
                        }
                    }
                    System.out.println("[ENV] Successfully loaded " + loadedCount + " property/properties from: " + candidate.toAbsolutePath().normalize());
                    break;
                } catch (Exception e) {
                    System.err.println("[ENV] Warning: Failed to parse .env file at " + candidate + ": " + e.getMessage());
                }
            }
        }
    }
}

