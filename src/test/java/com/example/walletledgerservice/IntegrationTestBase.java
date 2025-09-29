package com.example.walletledgerservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
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

    protected static void truncateSchema(JdbcTemplate jdbc, List<String> exclude) {
        List<String> tables = jdbc.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = ?",
                String.class, SCHEMA
        );

        if (exclude != null) tables.removeAll(exclude);
        if (tables.isEmpty()) return;

        String joined = tables.stream().map(t -> SCHEMA + "." + t).collect(Collectors.joining(","));
        jdbc.execute("TRUNCATE TABLE " + joined + " RESTART IDENTITY CASCADE");
    }

    protected ResultActions postJson(String url, Object body) throws Exception {
        return mockMvc.perform(
                post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
        );
    }

    ResultActions postJson(String url, Object body, Map<String, String> headers) throws Exception {
        var request = post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));

        if (headers != null) headers.forEach(request::header);
        return mockMvc.perform(request);
    }

    ResultActions postJson(String url, Map<String, String> headers, Map<String, String> param) throws Exception {
        var request = post(url)
                .accept(MediaType.APPLICATION_JSON);

        if (headers != null) headers.forEach(request::header);
        if (param != null)  param.forEach(request::param);

        return mockMvc.perform(request);
    }

    protected ResultActions getJson(String urlTemplate, Object... uriVars) throws Exception {
        return mockMvc.perform(
                get(urlTemplate, uriVars)
                        .accept(MediaType.APPLICATION_JSON)
        );
    }
}