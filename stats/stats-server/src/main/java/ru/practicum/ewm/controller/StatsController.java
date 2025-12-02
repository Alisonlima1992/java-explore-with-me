package ru.practicum.ewm.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.service.StatsService;
import ru.practicum.ewm.dto.EndpointHitDto;
import ru.practicum.ewm.dto.ViewStatsDto;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class StatsController {

    private final StatsService statsService;

    @PostMapping("/hit")
    public ResponseEntity<EndpointHitDto> saveHit(@Valid @RequestBody EndpointHitDto hitDto) {
        log.debug("postHit \n" + "EndpointHitDto \t{} \n", hitDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(statsService.postHit(hitDto));
    }

    @GetMapping("/stats")
    public List<ViewStatsDto> getStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") Boolean unique) {

        log.debug("getStats \n" + "start \t{} \n" + "end \t{} \n" + "uris \t{} \n" + "unique \t{} \n", start, end, uris, unique);

        return statsService.getStats(start, end, uris, unique);
    }
}