package com.backend.user.config;

import com.backend.user.util.SecretReader;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(
                ObjectUtils.asMap(
                        "cloud_name", SecretReader.readSecret("CLOUDINARY_CLOUD_NAME_FILE", "CLOUDINARY_CLOUD_NAME"),
                        "api_key", SecretReader.readSecret("CLOUDINARY_API_KEY_FILE", "CLOUDINARY_API_KEY"),
                        "api_secret", SecretReader.readSecret("CLOUDINARY_API_SECRET_FILE", "CLOUDINARY_API_SECRET"),
                        "secure", true
                )
        );
    }
}