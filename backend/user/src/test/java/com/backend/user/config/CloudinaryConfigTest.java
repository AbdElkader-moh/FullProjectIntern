package com.backend.user.config;

import com.backend.user.util.SecretReader;
import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class CloudinaryConfigTest {

    private final CloudinaryConfig config = new CloudinaryConfig();

    @Test
    void cloudinary_buildsClientFromSecretReaderValues() {
        try (MockedStatic<SecretReader> secretReader = mockStatic(SecretReader.class)) {
            secretReader.when(() -> SecretReader.readSecret("CLOUDINARY_CLOUD_NAME_FILE", "CLOUDINARY_CLOUD_NAME"))
                    .thenReturn("demo-cloud");
            secretReader.when(() -> SecretReader.readSecret("CLOUDINARY_API_KEY_FILE", "CLOUDINARY_API_KEY"))
                    .thenReturn("demo-key");
            secretReader.when(() -> SecretReader.readSecret("CLOUDINARY_API_SECRET_FILE", "CLOUDINARY_API_SECRET"))
                    .thenReturn("demo-secret");

            Cloudinary cloudinary = config.cloudinary();

            assertThat(cloudinary).isNotNull();
            assertThat(cloudinary.config.cloudName).isEqualTo("demo-cloud");
            assertThat(cloudinary.config.apiKey).isEqualTo("demo-key");
            assertThat(cloudinary.config.apiSecret).isEqualTo("demo-secret");
            assertThat(cloudinary.config.secure).isTrue();
        }
    }
}
