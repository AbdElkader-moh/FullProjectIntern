package com.backend.sensor_data.util;

import java.nio.file.Files;
import java.nio.file.Path;

public class SecretReader {

    public static String readSecret(String fileEnvName, String fallbackEnvName) {
        try {
            String filePath = System.getenv(fileEnvName);

            if (filePath != null && !filePath.isBlank()) {
                return Files.readString(Path.of(filePath)).trim();
            }

            return System.getenv(fallbackEnvName);

        } catch (Exception e) {
            throw new RuntimeException("Failed to read secret: " + fileEnvName, e);
        }
    }
}