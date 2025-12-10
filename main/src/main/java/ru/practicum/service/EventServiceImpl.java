package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.dto.event.UpdateEventRequest;
import ru.practicum.exception.*;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.*;
import ru.practicum.model.Event.EventState;
import ru.practicum.repository.*;
import ru.practicum.util.Constants;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;
    private final EventMapper eventMapper;
    private final StatsIntegrationService statsIntegrationService;

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        log.info("Creating event for user id: {}", userId);

        if (newEventDto.getParticipantLimit() < 0) {
            throw new ValidationException("Лимит участников не может быть отрицательным");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь", userId));

        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория", newEventDto.getCategory()));

        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(Constants.MIN_HOURS_BEFORE_EVENT))) {
            throw new ValidationException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
        }

        Event event = eventMapper.toEntity(newEventDto);
        event.setInitiator(user);
        event.setCategory(category);

        Event savedEvent = eventRepository.save(event);
        log.info("Event created with id: {}", savedEvent.getId());

        return eventMapper.toFullDto(savedEvent);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventRequest updateEvent) {
        log.info("Updating event id: {} by user id: {}", eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие", eventId));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие не принадлежит пользователю");
        }

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя изменить опубликованное событие");
        }

        updateEventFields(event, updateEvent);

        if (updateEvent.getStateAction() != null) {
            switch (updateEvent.getStateAction()) {
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
            }
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Event id: {} updated by user", eventId);

        return eventMapper.toFullDto(updatedEvent);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventRequest updateEvent) {
        log.info("Updating event id: {} by admin", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие", eventId));

        if (updateEvent.getStateAction() != null) {
            if (updateEvent.getStateAction() == UpdateEventRequest.StateAction.PUBLISH_EVENT) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Событие должно быть в состоянии ожидания публикации");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (updateEvent.getStateAction() == UpdateEventRequest.StateAction.REJECT_EVENT) {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Нельзя отклонить опубликованное событие");
                }
                event.setState(EventState.CANCELED);
            }
        }

        updateEventFields(event, updateEvent);

        Event updatedEvent = eventRepository.save(event);
        log.info("Event id: {} updated by admin", eventId);

        return eventMapper.toFullDto(updatedEvent);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size) {
        log.info("Getting events for user id: {}, from: {}, size: {}", userId, from, size);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь", userId);
        }

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());

        return eventRepository.findByInitiatorId(userId, pageable).getContent()
                .stream()
                .map(eventMapper::toShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        log.info("Getting event id: {} for user id: {}", eventId, userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие", eventId));

        return eventMapper.toFullDto(event);
    }

    @Override
    public List<EventFullDto> getEventsByAdmin(List<Long> users, List<String> states, List<Long> categories,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Integer from, Integer size) {
        log.info("Getting events by admin with filters: users={}, states={}, categories={}",
                users, states, categories);

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());

        List<EventState> eventStates = null;
        if (states != null && !states.isEmpty()) {
            try {
                eventStates = states.stream()
                        .map(String::toUpperCase)
                        .map(EventState::valueOf)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Некорректный статус события: " + e.getMessage());
            }
        }

        List<Long> usersParam = (users != null && !users.isEmpty()) ? users : null;
        List<Long> categoriesParam = (categories != null && !categories.isEmpty()) ? categories : null;

        try {
            List<Event> events = eventRepository.findEventsByAdmin(
                    usersParam,
                    eventStates,
                    categoriesParam,
                    rangeStart,
                    rangeEnd,
                    pageable
            ).getContent();

            return events.stream()
                    .map(eventMapper::toFullDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting events by admin: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при получении событий администратором: " + e.getMessage(), e);
        }
    }

    @Override
    public List<EventShortDto> getEventsByPublic(String text, List<Long> categories, Boolean paid,
                                                 LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                 Boolean onlyAvailable, String sort, Integer from, Integer size) {
        log.info("Getting events by public with filters");

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("Дата начала не может быть позже даты окончания");
        }

        String rangeStartStr = rangeStart != null ? rangeStart.format(FORMATTER) : null;
        String rangeEndStr = rangeEnd != null ? rangeEnd.format(FORMATTER) : null;

        if (rangeStartStr == null) {
            rangeStartStr = LocalDateTime.now().format(FORMATTER);
        }

        if (rangeEndStr == null) {
            rangeEndStr = LocalDateTime.now().plusYears(100).format(FORMATTER);
        }

        try {
            List<Event> events = eventRepository.findEventsByPublicNative(
                    text,
                    categories != null && !categories.isEmpty() ? categories : Collections.emptyList(),
                    paid,
                    rangeStartStr,
                    rangeEndStr,
                    from,
                    size
            );
            if (onlyAvailable != null && onlyAvailable) {
                events = events.stream()
                        .filter(event -> {
                            Integer confirmed = event.getConfirmedRequests() != null ? event.getConfirmedRequests() : 0;
                            Integer limit = event.getParticipantLimit() != null ? event.getParticipantLimit() : 0;
                            return limit == 0 || confirmed < limit;
                        })
                        .collect(Collectors.toList());
            }
            if ("VIEWS".equalsIgnoreCase(sort)) {
                events.sort((e1, e2) -> {
                    Long views1 = e1.getViews() != null ? e1.getViews() : 0L;
                    Long views2 = e2.getViews() != null ? e2.getViews() : 0L;
                    return views2.compareTo(views1); // по убыванию
                });
            }
            if (!events.isEmpty()) {
                List<String> uris = events.stream()
                        .map(e -> "/events/" + e.getId())
                        .collect(Collectors.toList());

                try {
                    Map<String, Long> views = statsIntegrationService.getViewsForUris(uris);
                    events.forEach(event -> {
                        Long viewCount = views.getOrDefault("/events/" + event.getId(), 0L);
                        event.setViews(viewCount);
                    });
                } catch (Exception e) {
                    log.warn("Failed to get views: {}", e.getMessage());
                }
            }

            return events.stream()
                    .map(eventMapper::toShortDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting events by public: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при получении событий: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public EventFullDto getEventByPublic(Long eventId) {
        log.info("Getting event id: {} by public", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие", eventId));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие", eventId);
        }

        statsIntegrationService.saveHit("/events/" + eventId, "127.0.0.1");

        Long views = statsIntegrationService.getViews("/events/" + eventId);

        if (views == null || views == 0) {
            views = 1L;
        } else {
            views = views + 1;
        }

        event.setViews(views);

        Integer confirmedRequests = requestRepository.countConfirmedRequestsByEventId(eventId);
        event.setConfirmedRequests(confirmedRequests);

        Event updatedEvent = eventRepository.save(event);

        return eventMapper.toFullDto(updatedEvent);
    }

    private void updateEventFields(Event event, UpdateEventRequest updateEvent) {
        if (updateEvent.getTitle() != null && !updateEvent.getTitle().isBlank()) {
            event.setTitle(updateEvent.getTitle());
        }

        if (updateEvent.getAnnotation() != null && !updateEvent.getAnnotation().isBlank()) {
            event.setAnnotation(updateEvent.getAnnotation());
        }

        if (updateEvent.getDescription() != null && !updateEvent.getDescription().isBlank()) {
            event.setDescription(updateEvent.getDescription());
        }

        if (updateEvent.getCategory() != null) {
            Category category = categoryRepository.findById(updateEvent.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория", updateEvent.getCategory()));
            event.setCategory(category);
        }

        if (updateEvent.getEventDate() != null) {
            if (updateEvent.getEventDate().isBefore(LocalDateTime.now().plusHours(Constants.MIN_HOURS_BEFORE_EVENT))) {
                throw new ValidationException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
            }
            event.setEventDate(updateEvent.getEventDate());
        }

        if (updateEvent.getLocation() != null) {
            event.setLocationLat(updateEvent.getLocation().getLat());
            event.setLocationLon(updateEvent.getLocation().getLon());
        }

        if (updateEvent.getPaid() != null) {
            event.setPaid(updateEvent.getPaid());
        }

        if (updateEvent.getParticipantLimit() != null) {
            if (updateEvent.getParticipantLimit() < 0) {
                throw new ValidationException("Лимит участников не может быть отрицательным");
            }
            event.setParticipantLimit(updateEvent.getParticipantLimit());
        }

        if (updateEvent.getRequestModeration() != null) {
            event.setRequestModeration(updateEvent.getRequestModeration());
        }
    }
}
