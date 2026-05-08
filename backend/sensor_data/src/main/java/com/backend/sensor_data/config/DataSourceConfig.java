package com.backend.sensor_data.config;

import com.backend.sensor_data.util.SecretReader;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();

        dataSource.setJdbcUrl(System.getenv("SPRING_DATASOURCE_URL"));

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