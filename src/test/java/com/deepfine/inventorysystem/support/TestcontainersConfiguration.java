package com.deepfine.inventorysystem.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * docker-compose와 같은 PostgreSQL 17을 띄운다. 락·UNIQUE·방언을 실제와 같게 검증하기 위함.
 * @ServiceConnection이 datasource url·계정을 주입한다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:17"))
                .withEnv("TZ", "Asia/Seoul")
                .withEnv("PGTZ", "Asia/Seoul");
    }
}
