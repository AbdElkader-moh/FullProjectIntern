package com.backend.user.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretReaderTest {

    @Test
    void constructor_isPrivate_andThrowsWhenInvoked() throws Exception {
        Constructor<SecretReader> constructor = SecretReader.class.getDeclaredConstructor();

        assertTrue(Modifier.isPrivate(constructor.getModifiers()),
                "SecretReader's constructor should be private");

        constructor.setAccessible(true);

        InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
                constructor::newInstance);
        assertTrue(thrown.getCause() instanceof UnsupportedOperationException);
    }

    @Test
    void readSecret_fallsBackToEnvVarWhenFileEnvNotSet() {
        // Neither env var exists in the test environment, so both should
        // resolve to null via the fallback path rather than throwing -
        // confirms the "file env not set" branch is exercised.
        String result = SecretReader.readSecret(
                "NON_EXISTENT_FILE_ENV_VAR_XYZ",
                "NON_EXISTENT_FALLBACK_ENV_VAR_XYZ");

        assertNull(result);
    }
}
