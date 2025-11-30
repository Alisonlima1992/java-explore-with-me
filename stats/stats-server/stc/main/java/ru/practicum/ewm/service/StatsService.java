    package stc.main.java.ru.practicum.ewm.service;

    import ru.practicum.ewm.dto.EndpointHitDto;
    import ru.practicum.ewm.dto.ViewStats;

    import java.time.LocalDateTime;
    import java.util.List;

    public interface StatsService {
        EndpointHitDto saveHit(EndpointHitDto hitDto);

        List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique);
    }