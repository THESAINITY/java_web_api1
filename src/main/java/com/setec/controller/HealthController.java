package com.setec.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "Product Web API");
        status.put("timestamp", java.time.LocalDateTime.now().toString());
        
        // Check database connection
        try (Connection conn = dataSource.getConnection()) {
            status.put("database", conn.getMetaData().getDatabaseProductName());
            status.put("databaseStatus", "CONNECTED");
        } catch (Exception e) {
            status.put("databaseStatus", "ERROR: " + e.getMessage());
        }
        
        return status;
    }

    @GetMapping("/")
    public String home() {
        return "Product Web API is running! Use /health to check status.";
    }
}