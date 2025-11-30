package stc.main.java.ru.practicum.statsserver.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.statsdto.EndpointHit;
import ru.practicum.statsdto.ViewStats;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StatsServiceImpl implements StatsService {

    private final Map<Long, EndpointHit> hitsStorage = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public EndpointHit saveHit(EndpointHit hit) {
        log.info("Saving hit: app={}, uri={}, ip={}", hit.getApp(), hit.getUri(), hit.getIp());

        Long id = idCounter.getAndIncrement();
        EndpointHit savedHit = EndpointHit.builder()
                .id(id)
                .app(hit.getApp())
                .uri(hit.getUri())
                .ip(hit.getIp())
                .timestamp(hit.getTimestamp())
                .build();

        hitsStorage.put(id, savedHit);
        log.info("Hit saved successfully with id: {}", id);

        return savedHit;
    }

    @Override
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        log.info("Getting stats: start={}, end={}, uris={}, unique={}", start, end, uris, unique);

        List<EndpointHit> filteredHits = hitsStorage.values().stream()
                .filter(hit -> isInTimeRange(hit.getTimestamp(), start, end))
                .filter(hit -> uris == null || uris.isEmpty() || uris.contains(hit.getUri()))
                .collect(Collectors.toList());

        log.info("Found {} hits after filtering", filteredHits.size());

        Map<String, Map<String, StatsCounter>> statsByAppAndUri = new HashMap<>();

        for (EndpointHit hit : filteredHits) {
            String app = hit.getApp();
            String uri = hit.getUri();
            String ip = hit.getIp();

            statsByAppAndUri
                    .computeIfAbsent(app, k -> new HashMap<>())
                    .computeIfAbsent(uri, k -> new StatsCounter())
                    .addHit(ip, unique);
        }

        List<ViewStats> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, StatsCounter>> appEntry : statsByAppAndUri.entrySet()) {
            for (Map.Entry<String, StatsCounter> uriEntry : appEntry.getValue().entrySet()) {
                long hitsCount = unique ? uriEntry.getValue().getUniqueHits() : uriEntry.getValue().getTotalHits();

                result.add(ViewStats.builder()
                        .app(appEntry.getKey())
                        .uri(uriEntry.getKey())
                        .hits(hitsCount)
                        .build());
            }
        }

        result.sort((a, b) -> Long.compare(b.getHits(), a.getHits()));

        log.info("Returning {} stats records", result.size());
        return result;
    }

    private boolean isInTimeRange(LocalDateTime timestamp, LocalDateTime start, LocalDateTime end) {
        boolean afterStart = start == null || !timestamp.isBefore(start);
        boolean beforeEnd = end == null || !timestamp.isAfter(end);
        return afterStart && beforeEnd;
    }

    private static class StatsCounter {
        private final Set<String> uniqueIps = new HashSet<>();
        private long totalHits = 0;

        void addHit(String ip, Boolean unique) {
            totalHits++;
            if (Boolean.TRUE.equals(unique)) {
                uniqueIps.add(ip);
            }
        }

        long getTotalHits() {
            return totalHits;
        }

        long getUniqueHits() {
            return uniqueIps.size();
        }
    }
}