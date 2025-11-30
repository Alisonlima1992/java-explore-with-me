package stc.main.java.ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stc.main.java.ru.practicum.ewm.exception.InvalidDateRangeException;
import stc.main.java.ru.practicum.ewm.mapper.EndpointHitMapper;
import stc.main.java.ru.practicum.ewm.model.EndpointHit;
import stc.main.java.ru.practicum.ewm.repository.StatsRepository;
import ru.practicum.ewm.dto.EndpointHitDto;
import ru.practicum.ewm.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;

    @Override
    @Transactional
    public EndpointHitDto saveHit(EndpointHitDto hitDto) {
        log.info("Saving hit to database: app={}, uri={}, ip={}",
                hitDto.getApp(), hitDto.getUri(), hitDto.getIp());

        EndpointHit hit = EndpointHitMapper.toEntity(hitDto);
        EndpointHit savedHit = statsRepository.save(hit);
        EndpointHitDto savedHitDto = EndpointHitMapper.toDto(savedHit);

        log.info("Hit saved successfully with id: {}", savedHitDto.getId());
        return savedHitDto;
    }

    @Override
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        log.info("Getting stats from database: start={}, end={}, uris={}, unique={}", start, end, uris, unique);

        validateDateRange(start, end);

        if (uris == null || uris.isEmpty()) {
            if (Boolean.TRUE.equals(unique)) {
                return statsRepository.getUniqueStats(start, end);
            } else {
                return statsRepository.getStats(start, end);
            }
        } else {
            if (Boolean.TRUE.equals(unique)) {
                return statsRepository.getUniqueStatsByUris(start, end, uris);
            } else {
                return statsRepository.getStatsByUris(start, end, uris);
            }
        }
    }

    private void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new InvalidDateRangeException("Start date must be before end date");
        }
    }
}