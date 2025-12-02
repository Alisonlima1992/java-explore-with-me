package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.EndpointHitDto;
import ru.practicum.ewm.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsService {

    public EndpointHitDto postHit(EndpointHitDto hitDto);

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique);

}