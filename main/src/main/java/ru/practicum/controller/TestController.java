package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.statsclient.StatsClient;
import ru.practicum.statsdto.EndpointHit;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final StatsClient statsClient;

    @GetMapping("/test")
    public Map<String, String> test() {
        log.info("Received test request");

        EndpointHit hit = EndpointHit.builder()
                .app("ewm-main-service")
                .uri("/test")
                .ip("127.0.0.1")
                .timestamp(LocalDateTime.now())
                .build();

        try {
            statsClient.hit(hit);
            log.info("Successfully sent hit to stats service");
        } catch (Exception e) {
            log.warn("Failed to send hit to stats service: {}", e.getMessage());
        }

        return Collections.singletonMap("message", "EWM Service is working!");
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Collections.singletonMap("status", "OK");
    }
}