package ru.practicum.service;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.dto.event.UpdateEventRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {

    EventFullDto createEvent(Long userId, NewEventDto newEventDto);

    EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventRequest updateEvent);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventRequest updateEvent);

    List<EventShortDto> getUserEvents(Long userId, @Min(0) Integer from, @Positive Integer size);

    EventFullDto getUserEventById(Long userId, Long eventId);

    List<EventFullDto> getEventsByAdmin(List<Long> users, List<String> states, List<Long> categories,
                                        LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                        @Min(0) Integer from, @Positive Integer size);

    List<EventShortDto> getEventsByPublic(String text, List<Long> categories, Boolean paid,
                                          LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                          Boolean onlyAvailable, String sort,
                                          @Min(0) Integer from, @Positive Integer size);

    EventFullDto getEventByPublic(Long eventId);
}