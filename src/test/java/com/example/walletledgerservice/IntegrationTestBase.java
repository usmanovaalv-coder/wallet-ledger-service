package com.example.walletledgerservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationTestBase {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("wallet_db")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true);

    protected static final String SCHEMA = "wallet_schema";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        POSTGRES.start();
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);

        registry.add("spring.liquibase.default-schema", () -> SCHEMA);
        registry.add("spring.liquibase.liquibase-schema", () -> "public");
    }

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanDb() {
        truncateSchema(jdbc, List.of("databasechangelog", "databasechangeloglock"));
    }

    protected static void truncateSchema(JdbcTemplate jdbc,
                                         @Nullable List<String> excludeTables) {
        List<String> tables = jdbc.queryForList("""
            SELECT tablename
            FROM pg_tables
            WHERE schemaname = ?
              AND tablename NOT IN ('databasechangelog','databasechangeloglock')
            """,
                String.class, SCHEMA);

        if (excludeTables != null && !excludeTables.isEmpty()) {
            tables.removeAll(excludeTables);
        }

        if (!tables.isEmpty()) {
            String joined = tables.stream()
                    .map(t -> SCHEMA + "." + t)
                    .collect(Collectors.joining(","));
            jdbc.execute("TRUNCATE TABLE " + joined + " RESTART IDENTITY CASCADE");
        }
    }

    protected ResultActions postJson(String url, Object body) throws Exception {
        return mockMvc.perform(
                post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
        );
    }

    protected ResultActions getJson(String urlTemplate, Object... uriVars) throws Exception {
        return mockMvc.perform(
                get(urlTemplate, uriVars)
                        .accept(MediaType.APPLICATION_JSON)
        );
    }
}