package com.backend.user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    void userServiceOpenAPI_setsExpectedInfo() {
        OpenAPI openAPI = config.userServiceOpenAPI();

        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("User Microservice API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0.0");
        assertThat(openAPI.getInfo().getDescription())
                .contains("signup", "login", "profile", "settings", "notifications");
        assertThat(openAPI.getInfo().getContact()).isNotNull();
        assertThat(openAPI.getInfo().getContact().getName()).isEqualTo("DXC Internship Team");
    }

    @Test
    void userServiceOpenAPI_setsExpectedServers() {
        OpenAPI openAPI = config.userServiceOpenAPI();

        assertThat(openAPI.getServers()).hasSize(2);

        Server localDev = openAPI.getServers().get(0);
        assertThat(localDev.getUrl()).isEqualTo("http://localhost:8080");
        assertThat(localDev.getDescription()).isEqualTo("Local dev server");

        Server viaProxy = openAPI.getServers().get(1);
        assertThat(viaProxy.getUrl()).isEqualTo("http://localhost:4200");
        assertThat(viaProxy.getDescription()).isEqualTo("Via Nginx proxy");
    }
}
