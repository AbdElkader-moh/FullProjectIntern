package com.backend.user.config;

import com.backend.user.util.SecretReader;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class DataSourceConfigTest {

    private final DataSourceConfig config = new DataSourceConfig();

    // Note: java.lang.System itself can't be mocked (Mockito blocks it to avoid
    // classloading deadlocks), so this test doesn't stub System.getenv. Instead it
    // asserts the bean passes through whatever SPRING_DATASOURCE_URL actually
    // resolves to in this JVM (often null/unset in a plain unit-test run) rather
    // than a fixed expected value — the point of the test is the wiring, not the
    // literal URL string. SecretReader is still mockable, so username/password/driver
    // are asserted against known fixed values.
    @Test
    void dataSource_setsJdbcUrlUsernamePasswordAndDriver() {
    try (MockedStatic<SecretReader> secretReader = mockStatic(SecretReader.class)) {

        secretReader.when(() -> SecretReader.readSecret("SPRING_DATASOURCE_USERNAME_FILE", "SPRING_DATASOURCE_USERNAME"))
                .thenReturn("test-user");
        secretReader.when(() -> SecretReader.readSecret("SPRING_DATASOURCE_PASSWORD_FILE", "SPRING_DATASOURCE_PASSWORD"))
                .thenReturn("test-pass");

        DataSource dataSource = config.dataSource();

        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
        HikariDataSource hikari = (HikariDataSource) dataSource;

        String expectedUrl = System.getenv("SPRING_DATASOURCE_URL");
        if (expectedUrl == null || expectedUrl.isBlank()) {
            expectedUrl = "jdbc:mysql://localhost:3307/project_db";
        }
        assertThat(hikari.getJdbcUrl()).isEqualTo(expectedUrl);
        assertThat(hikari.getUsername()).isEqualTo("test-user");
        assertThat(hikari.getPassword()).isEqualTo("test-pass");
        assertThat(hikari.getDriverClassName()).isEqualTo("com.mysql.cj.jdbc.Driver");
    }
    }

    @Test
    void dataSource_returnsHikariDataSourceInstance() {
        try (MockedStatic<SecretReader> secretReader = mockStatic(SecretReader.class)) {
            secretReader.when(() -> SecretReader.readSecret("SPRING_DATASOURCE_USERNAME_FILE", "SPRING_DATASOURCE_USERNAME"))
                    .thenReturn("any-user");
            secretReader.when(() -> SecretReader.readSecret("SPRING_DATASOURCE_PASSWORD_FILE", "SPRING_DATASOURCE_PASSWORD"))
                    .thenReturn("any-pass");

            DataSource dataSource = config.dataSource();

            assertThat(dataSource).isNotNull();
            assertThat(dataSource).isInstanceOf(HikariDataSource.class);
        }
    }
}
