package com.backend.sensor_data.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sensorServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sensor Data Microservice API")
                        .description("REST API for sensor data ingestion and alerts — traffic, air pollution, and street lights.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("DXC Internship Team")))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Local dev server"),
                        new Server().url("http://localhost:4200").description("Via Nginx proxy")));
    }
}
