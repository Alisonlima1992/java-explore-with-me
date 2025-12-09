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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

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

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь", userId));

        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория", newEventDto.getCategory()));

        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(Constants.MIN_HOURS_BEFORE_EVENT))) {
            throw new ValidationException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
        }

        if (newEventDto.getParticipantLimit() != null && newEventDto.getParticipantLimit() < 0) {
            throw new ValidationException("Лимит участников не может быть отрицательным");
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

        if (updateEvent.getEventDate() != null) {
            if (updateEvent.getEventDate().isBefore(LocalDateTime.now().plusHours(Constants.MIN_HOURS_BEFORE_EVENT))) {
                throw new ValidationException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
            }
        }


        if (updateEvent.getParticipantLimit() != null && updateEvent.getParticipantLimit() < 0) {
            throw new ValidationException("Лимит участников не может быть отрицательным");
        }

        if (updateEvent.getParticipantLimit() != null) {
            int confirmedRequests = requestRepository.countConfirmedRequestsByEventId(eventId);
            if (updateEvent.getParticipantLimit() != 0 && confirmedRequests > updateEvent.getParticipantLimit()) {
                throw new ConflictException("Нельзя установить лимит участников меньше количества уже подтвержденных запросов");
            }
        }

        updateEventFields(event, updateEvent);

        if (updateEvent.getStateAction() != null) {
            switch (updateEvent.getStateAction()) {
                case SEND_TO_REVIEW:
                    if (event.getState() != EventState.CANCELED) {
                        event.setState(EventState.PENDING);
                    } else {
                        throw new ConflictException("Нельзя отправить на модерацию отмененное событие");
                    }
                    break;
                case CANCEL_REVIEW:
                    if (event.getState() != EventState.PUBLISHED) {
                        event.setState(EventState.CANCELED);
                    } else {
                        throw new ConflictException("Нельзя отменить опубликованное событие");
                    }
                    break;
                default:
                    throw new ValidationException("Некорректное действие: " + updateEvent.getStateAction());
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
        log.info("Getting events by admin with filters");

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());

        List<EventState> eventStates = null;
        if (states != null && !states.isEmpty()) {
            eventStates = states.stream()
                    .map(EventState::valueOf)
                    .collect(Collectors.toList());
        }

        List<Event> events = eventRepository.findEventsByAdmin(
                users, eventStates, categories, rangeStart, rangeEnd, pageable
        ).getContent();

        return events.stream()
                .map(eventMapper::toFullDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventShortDto> getEventsByPublic(String text, List<Long> categories, Boolean paid,
                                                 LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                 Boolean onlyAvailable, String sort, Integer from, Integer size) {
        log.info("Getting events by public with filters: text={}, categories={}, paid={}, rangeStart={}, rangeEnd={}, onlyAvailable={}, sort={}, from={}, size={}",
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);

        if (from < 0) {
            throw new ValidationException("Параметр 'from' не может быть отрицательным");
        }

        if (size <= 0) {
            throw new ValidationException("Параметр 'size' должен быть положительным числом");
        }

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("Дата начала не может быть позже даты окончания");
        }

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
        }

        Boolean finalOnlyAvailable = (onlyAvailable != null) ? onlyAvailable : false;

        Sort sorting = Sort.by("eventDate").ascending();
        if (sort != null) {
            if ("VIEWS".equalsIgnoreCase(sort)) {
                sorting = Sort.by("views").descending();
            } else if ("EVENT_DATE".equalsIgnoreCase(sort)) {
                sorting = Sort.by("eventDate").descending();
            }
        }

        int page = (from > 0 && size > 0) ? from / size : 0;
        Pageable pageable = PageRequest.of(page, size, sorting);

        log.debug("Pageable created: page={}, size={}, sort={}", page, size, sorting);

        List<Event> events = eventRepository.findEventsByPublic(
                text, categories, paid, rangeStart, rangeEnd, finalOnlyAvailable, pageable
        ).getContent();

        log.debug("Found {} events", events.size());

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> uris = events.stream()
                .map(e -> "/events/" + e.getId())
                .collect(Collectors.toList());

        log.debug("Fetching views for URIs: {}", uris);

        Map<String, Long> views = statsIntegrationService.getViewsForUris(uris);

        log.debug("Views map: {}", views);

        events.forEach(event -> {
            String eventUri = "/events/" + event.getId();
            Long viewCount = views.getOrDefault(eventUri, 0L);
            log.debug("Event {} has {} views", event.getId(), viewCount);
            event.setViews(viewCount);
        });

        List<EventShortDto> result = events.stream()
                .map(eventMapper::toShortDto)
                .collect(Collectors.toList());

        log.debug("Returning {} events", result.size());

        return result;
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
        event.setViews(views);

        Event updatedEvent = eventRepository.save(event);

        return eventMapper.toFullDto(updatedEvent);
    }

    private void updateEventFields(Event event, UpdateEventRequest updateEvent) {
        if (updateEvent.getTitle() != null && !updateEvent.getTitle().isBlank()) {
            if (updateEvent.getTitle().length() < 3 || updateEvent.getTitle().length() > 120) {
                throw new ValidationException("Заголовок должен быть от 3 до 120 символов");
            }
            event.setTitle(updateEvent.getTitle());
        }

        if (updateEvent.getAnnotation() != null && !updateEvent.getAnnotation().isBlank()) {
            if (updateEvent.getAnnotation().length() < 20 || updateEvent.getAnnotation().length() > 2000) {
                throw new ValidationException("Аннотация должна быть от 20 до 2000 символов");
            }
            event.setAnnotation(updateEvent.getAnnotation());
        }

        if (updateEvent.getDescription() != null && !updateEvent.getDescription().isBlank()) {
            if (updateEvent.getDescription().length() < 20 || updateEvent.getDescription().length() > 7000) {
                throw new ValidationException("Описание должно быть от 20 до 7000 символов");
            }
            event.setDescription(updateEvent.getDescription());
        }

        if (updateEvent.getCategory() != null) {
            Category category = categoryRepository.findById(updateEvent.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория", updateEvent.getCategory()));
            event.setCategory(category);
        }

        if (updateEvent.getEventDate() != null) {
            event.setEventDate(updateEvent.getEventDate());
        }

        if (updateEvent.getLocation() != null) {
            if (updateEvent.getLocation().getLat() == null || updateEvent.getLocation().getLon() == null) {
                throw new ValidationException("Координаты локации обязательны");
            }
            event.setLocationLat(updateEvent.getLocation().getLat());
            event.setLocationLon(updateEvent.getLocation().getLon());
        }

        if (updateEvent.getPaid() != null) {
            event.setPaid(updateEvent.getPaid());
        }

        if (updateEvent.getParticipantLimit() != null) {
            event.setParticipantLimit(updateEvent.getParticipantLimit());
        }

        if (updateEvent.getRequestModeration() != null) {
            event.setRequestModeration(updateEvent.getRequestModeration());
        }
    }
}
