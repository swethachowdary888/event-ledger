package com.eventledger.account.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<>();

        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);

            response.put("status", "UP");
            response.put("service", "account-service");
            response.put("database", "UP");
        } catch (Exception exception) {
            response.put("status", "DOWN");
            response.put("service", "account-service");
            response.put("database", "DOWN");
        }

        response.put("timestamp", Instant.now());

        return response;
    }
}