package com.backend.sensor_data.util;

import java.nio.file.Files;
import java.nio.file.Paths;

public class SecretReader {

    public static String readSecret(String fileEnvName, String fallbackEnvName) {
        try {
            String filePath = System.getenv(fileEnvName);

            if (filePath != null && !filePath.isBlank()) {
                return Files.readString(Paths.get(filePath)).trim();
            }

            return System.getenv(fallbackEnvName);

        } catch (java.io.IOException | SecurityException e) {
            throw new RuntimeException("Failed to read secret: " + fileEnvName, e);
        }
    }
}