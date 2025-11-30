package ru.practicum.ewm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import ru.practicum.ewm.dto.EndpointHit;
import ru.practicum.ewm.dto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
public class StatsClient {
    private final RestTemplate rest;
    private final String serverUrl;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsClient(String serverUrl) {
        this.rest = new RestTemplate();
        this.serverUrl = serverUrl;
    }

    public void hit(EndpointHit hit) {
        HttpEntity<EndpointHit> requestEntity = new HttpEntity<>(hit, defaultHeaders());

        try {
            ResponseEntity<Object> response = rest.exchange(
                    serverUrl + "/hit",
                    HttpMethod.POST,
                    requestEntity,
                    Object.class
            );
            log.info("Hit recorded successfully: {}", hit.getUri());
        } catch (HttpStatusCodeException e) {
            log.error("Failed to record hit: {}", e.getMessage());
            throw new RuntimeException("Failed to record hit: " + e.getMessage(), e);
        }
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end,
                                    List<String> uris, Boolean unique) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("start", start.format(formatter));
        parameters.put("end", end.format(formatter));
        parameters.put("unique", unique != null ? unique : false);

        StringBuilder path = new StringBuilder(serverUrl + "/stats?start={start}&end={end}&unique={unique}");

        if (uris != null && !uris.isEmpty()) {
            path.append("&uris={uris}");
            parameters.put("uris", String.join(",", uris));
        }

        try {
            ResponseEntity<ViewStats[]> response = rest.exchange(
                    path.toString(),
                    HttpMethod.GET,
                    new HttpEntity<>(defaultHeaders()),
                    ViewStats[].class,
                    parameters
            );

            ViewStats[] body = response.getBody();
            return body != null ? Arrays.asList(body) : Collections.emptyList();
        } catch (HttpStatusCodeException e) {
            log.error("Failed to get stats: {}", e.getMessage());
            throw new RuntimeException("Failed to get stats: " + e.getMessage(), e);
        }
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}