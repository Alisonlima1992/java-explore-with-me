package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.client.StatsClient;
import ru.practicum.ewm.dto.EndpointHitDto;
import ru.practicum.ewm.dto.ViewStatsDto;
import ru.practicum.util.Constants;

import java.time.LocalDateTime;
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
            log.error("Failed to save hit for uri: {}, ip: {}. Error: {}", uri, ip, e.getMessage());
        }
    }

    public Long getViews(String uri) {
        try {
            LocalDateTime start = LocalDateTime.now().minusYears(1);
            LocalDateTime end = LocalDateTime.now();

            List<ViewStatsDto> stats = statsClient.getStats(
                    start, end, List.of(uri), true
            );

            return stats.isEmpty() ? 0L : stats.get(0).getHits();
        } catch (Exception e) {
            log.error("Failed to get views for uri: {}. Error: {}", uri, e.getMessage());
            return 0L;
        }
    }

    public Map<String, Long> getViewsForUris(List<String> uris) {
        try {
            LocalDateTime start = LocalDateTime.now().minusYears(1);
            LocalDateTime end = LocalDateTime.now();

            List<ViewStatsDto> stats = statsClient.getStats(
                    start, end, uris, true
            );

            return stats.stream()
                    .collect(Collectors.toMap(ViewStatsDto::getUri, ViewStatsDto::getHits));
        } catch (Exception e) {
            log.error("Failed to get views for uris: {}. Error: {}", uris, e.getMessage());
            return Map.of();
        }
    }
}