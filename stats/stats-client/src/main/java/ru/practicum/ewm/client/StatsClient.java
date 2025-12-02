package ru.practicum.ewm.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.ewm.dto.EndpointHitDto;
import ru.practicum.ewm.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class StatsClient {
    private final RestTemplate restTemplate;
    private final String serverUrl;

    public StatsClient(RestTemplateBuilder builder, @Value("${ewm-main-service.url}") String serverUrl) {
        this.serverUrl = serverUrl;
        this.restTemplate = builder
                .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl))
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
                .build();
    }

    public ResponseEntity<EndpointHitDto> postHit(EndpointHitDto dto) {
        return restTemplate.postForEntity(
                serverUrl + "/hit",
                dto,
                EndpointHitDto.class
        );
    }


    public List<ViewStatsDto> getStats(LocalDateTime start,
                                       LocalDateTime end,
                                       List<String> uris,
                                       Boolean unique) {
        if (start == null) {
            throw new IllegalArgumentException("Параметр Start должен быть задан");
        }
        if (end == null) {
            throw new IllegalArgumentException("Параметр End должен быть задан");
        }

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(serverUrl + "/stats")
                .queryParam("start", start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .queryParam("end", end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        if (uris != null && !uris.isEmpty()) {
            for (String uri : uris) {
                builder.queryParam("uris", uri);
            }
        }

        if (unique != null) {
            builder.queryParam("unique", unique);
        }

        String url = builder.toUriString();

        ParameterizedTypeReference<List<ViewStatsDto>> responseType =
                new ParameterizedTypeReference<List<ViewStatsDto>>() {
                };

        ResponseEntity<List<ViewStatsDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                responseType);

        if (response.getStatusCode().isError()) {
            throw new RuntimeException("Запрос не выполнился, код статуса " + response.getStatusCode());
        }

        return response.getBody();
    }
}