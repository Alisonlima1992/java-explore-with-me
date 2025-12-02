package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.EndpointHitDto;
import ru.practicum.ewm.dto.ViewStatsDto;
import ru.practicum.ewm.mapper.EndpointHitMapper;
import ru.practicum.ewm.model.EndpointHit;
import ru.practicum.ewm.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;
    private final EndpointHitMapper mapper;

    @Transactional
    @Override
    public EndpointHitDto postHit(EndpointHitDto hitDto) {
        EndpointHit endpointHit = statsRepository.save(mapper.toEntity(hitDto));
        return mapper.toDto(endpointHit);
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {

        if (start.isAfter(end) || start.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Указана некорректная дата");
        }

        if (unique) {
            return findUniqueIpStats(start, end, uris);
        } else {
            return findNonUniqueIpStats(start, end, uris);
        }
    }

    private List<ViewStatsDto> findUniqueIpStats(LocalDateTime start, LocalDateTime end, List<String> uris) {
        return uris == null
                ? statsRepository.getStatsWithUniqueIp(start, end)
                : statsRepository.getStatsByUrisWithUniqueIp(start, end, uris);
    }

    private List<ViewStatsDto> findNonUniqueIpStats(LocalDateTime start, LocalDateTime end, List<String> uris) {
        return uris == null
                ? statsRepository.getStats(start, end)
                : statsRepository.getStatsByUris(start, end, uris);
    }
}