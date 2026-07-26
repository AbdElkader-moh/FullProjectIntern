package com.backend.sensor_data.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

/**
 * Covers SecretReader.readSecret()'s 4 branch conditions:
 *   1. file-env-var set + points to a readable file      -> returns trimmed file contents
 *   2. file-env-var unset/blank                            -> falls back to fallbackEnvName's value
 *   3. file-env-var set but points to a missing/bad path   -> wraps IOException in RuntimeException
 *   4. file-env-var set but points to a directory          -> wraps IOException in RuntimeException
 *      (this is the exact failure mode from the real jwt_secret.txt incident)
 * plus the private constructor's guard clause.
 *
 * NOTE: System.getenv() can't be mutated directly in the JVM, so this uses
 * system-stubs-jupiter to stub environment variables for the test process.
 * Add to pom.xml (test scope) if not already present:
 *
 *   <dependency>
 *       <groupId>uk.org.webcompere</groupId>
 *       <artifactId>system-stubs-jupiter</artifactId>
 *       <version>2.1.7</version>
 *       <scope>test</scope>
 *   </dependency>
 */
@ExtendWith(SystemStubsExtension.class)
class SecretReaderTest {

    @SystemStub
    private EnvironmentVariables environmentVariables;

    private static final String FILE_ENV = "TEST_SECRET_FILE";
    private static final String FALLBACK_ENV = "TEST_SECRET_FALLBACK";

    // ---------------- private constructor ----------------

    @Test
    void constructor_isPrivate_andThrowsIllegalStateException() throws Exception {
        Constructor<SecretReader> constructor = SecretReader.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    // ---------------- happy path: reads from file ----------------

    @Test
    void readSecret_fileEnvPointsToReadableFile_returnsTrimmedContents(@TempDir Path tempDir) throws IOException {
        Path secretFile = tempDir.resolve("secret.txt");
        Files.writeString(secretFile, "  my-super-secret-value  \n");

        environmentVariables.set(FILE_ENV, secretFile.toString());
        environmentVariables.set(FALLBACK_ENV, "should-not-be-used");

        String result = SecretReader.readSecret(FILE_ENV, FALLBACK_ENV);

        assertThat(result).isEqualTo("my-super-secret-value");
    }

    // ---------------- fallback: file env unset or blank ----------------

    @Test
    void readSecret_fileEnvUnset_fallsBackToFallbackEnvValue() {
        environmentVariables.set(FILE_ENV, null);
        environmentVariables.set(FALLBACK_ENV, "fallback-secret-value");

        String result = SecretReader.readSecret(FILE_ENV, FALLBACK_ENV);

        assertThat(result).isEqualTo("fallback-secret-value");
    }

    @Test
    void readSecret_fileEnvBlank_fallsBackToFallbackEnvValue() {
        environmentVariables.set(FILE_ENV, "   ");
        environmentVariables.set(FALLBACK_ENV, "fallback-secret-value");

        String result = SecretReader.readSecret(FILE_ENV, FALLBACK_ENV);

        assertThat(result).isEqualTo("fallback-secret-value");
    }

    @Test
    void readSecret_bothEnvVarsUnset_returnsNull() {
        environmentVariables.set(FILE_ENV, null);
        environmentVariables.set(FALLBACK_ENV, null);

        String result = SecretReader.readSecret(FILE_ENV, FALLBACK_ENV);

        assertThat(result).isNull();
    }

    // ---------------- failure paths ----------------

    @Test
    void readSecret_fileEnvPointsToNonExistentPath_throwsRuntimeExceptionWrappingIOException() {
        environmentVariables.set(FILE_ENV, "/nonexistent/path/does-not-exist.txt");

        assertThatThrownBy(() -> SecretReader.readSecret(FILE_ENV, FALLBACK_ENV))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to read secret: " + FILE_ENV)
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void readSecret_fileEnvPointsToDirectory_throwsRuntimeExceptionWrappingIOException(@TempDir Path tempDir) {
        // Reproduces the exact real-world incident: Docker bind-mounting a
        // not-yet-created host file creates a directory in its place.
        environmentVariables.set(FILE_ENV, tempDir.toString());

        assertThatThrownBy(() -> SecretReader.readSecret(FILE_ENV, FALLBACK_ENV))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to read secret: " + FILE_ENV)
                .hasCauseInstanceOf(IOException.class);
    }
}
