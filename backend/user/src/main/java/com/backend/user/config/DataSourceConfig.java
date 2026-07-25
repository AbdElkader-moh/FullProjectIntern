package com.backend.user.config;

import com.backend.user.util.SecretReader;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();

        String jdbcUrl = System.getenv("SPRING_DATASOURCE_URL");
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            jdbcUrl = "jdbc:mysql://localhost:3307/project_db";
        }
        dataSource.setJdbcUrl(jdbcUrl);

        dataSource.setUsername(
            SecretReader.readSecret(
                "SPRING_DATASOURCE_USERNAME_FILE",
                "SPRING_DATASOURCE_USERNAME"
            )
        );

        dataSource.setPassword(
            SecretReader.readSecret(
                "SPRING_DATASOURCE_PASSWORD_FILE",
                "SPRING_DATASOURCE_PASSWORD"
            )
        );

        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

        return dataSource;
    }
}