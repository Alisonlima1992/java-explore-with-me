package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.client.StatsClient;
import ru.practicum.ewm.dto.EndpointHitDto;
import ru.practicum.ewm.dto.ViewStatsDto;
import ru.practicum.util.Constants;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsIntegrationService {

    private final StatsClient statsClient;

    public void saveHit(String uri, String ip) {
        try {
            EndpointHitDto hitDto = new EndpointHitDto();
            hitDto.setApp(Constants.APP_NAME);
            hitDto.setUri(uri);
            hitDto.setIp(ip);
            hitDto.setTimestamp(LocalDateTime.now());

            statsClient.postHit(hitDto);
            log.debug("Hit saved for uri: {}, ip: {}", uri, ip);
        } catch (Exception e) {
            log.error("Failed to save hit for uri: {}, ip: {}. Error: {}", uri, ip, e.getMessage(), e);
        }
    }

    public Long getViews(String uri) {
        try {
            LocalDateTime start = LocalDateTime.now().minusYears(1);
            LocalDateTime end = LocalDateTime.now();

            List<ViewStatsDto> stats = statsClient.getStats(
                    start, end, List.of(uri), true
            );

            if (stats == null || stats.isEmpty()) {
                return 0L;
            }

            return stats.stream()
                    .filter(s -> uri.equals(s.getUri()))
                    .map(ViewStatsDto::getHits)
                    .findFirst()
                    .orElse(0L);

        } catch (Exception e) {
            log.error("Failed to get views for uri: {}. Error: {}", uri, e.getMessage(), e);
            return 0L;
        }
    }

    public Map<String, Long> getViewsForUris(List<String> uris) {
        try {
            if (uris == null || uris.isEmpty()) {
                return new HashMap<>();
            }

            LocalDateTime start = LocalDateTime.now().minusYears(1);
            LocalDateTime end = LocalDateTime.now();

            List<ViewStatsDto> stats = statsClient.getStats(
                    start, end, uris, true
            );

            if (stats == null || stats.isEmpty()) {
                return uris.stream()
                        .collect(Collectors.toMap(uri -> uri, uri -> 0L));
            }

            Map<String, Long> result = new HashMap<>();

            uris.forEach(uri -> result.put(uri, 0L));

            stats.forEach(stat -> {
                if (uris.contains(stat.getUri())) {
                    result.put(stat.getUri(), stat.getHits());
                }
            });

            return result;

        } catch (Exception e) {
            log.error("Failed to get views for uris: {}. Error: {}", uris, e.getMessage(), e);
            return uris.stream()
                    .collect(Collectors.toMap(uri -> uri, uri -> 0L));
        }
    }
}