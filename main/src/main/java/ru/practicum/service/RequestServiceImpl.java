package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.exception.*;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.*;
import ru.practicum.model.Event.EventState;
import ru.practicum.model.ParticipationRequest.RequestStatus;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.RequestRepository;
import ru.practicum.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RequestMapper requestMapper;

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.info("Creating request for event id: {} by user id: {}", eventId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь", userId));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие", eventId));

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Нельзя добавить повторный запрос");
        }

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может добавить запрос на участие в своём событии");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        int confirmedRequests = requestRepository.countConfirmedRequestsByEventId(eventId);
        if (event.getParticipantLimit() != 0 && confirmedRequests >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит запросов на участие");
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .event(event)
                .requester(user)
                .created(LocalDateTime.now())
                .build();

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
            event.setConfirmedRequests(confirmedRequests + 1);
            eventRepository.save(event);
        } else {
            request.setStatus(RequestStatus.PENDING);
        }

        ParticipationRequest savedRequest = requestRepository.save(request);
        log.info("Request created with id: {}", savedRequest.getId());

        return requestMapper.toDto(savedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.info("Getting requests for user id: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь", userId);
        }

        return requestRepository.findByRequesterId(userId)
                .stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Cancelling request id: {} by user id: {}", requestId, userId);

        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос", requestId));

        if (!request.getRequester().getId().equals(userId)) {
            throw new NotFoundException("Запрос не принадлежит пользователю");
        }

        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest updatedRequest = requestRepository.save(request);

        log.info("Request id: {} cancelled", requestId);
        return requestMapper.toDto(updatedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.info("Getting requests for event id: {} by user id: {}", eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие", eventId));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие не принадлежит пользователю");
        }

        return requestRepository.findByEventId(eventId)
                .stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ParticipationRequestDto> updateRequestStatuses(Long userId, Long eventId, List<Long> requestIds, String status) {
        log.info("Updating request statuses for event id: {} by user id: {}", eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие", eventId));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие не принадлежит пользователю");
        }

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            throw new ConflictException("Подтверждение заявок не требуется");
        }

        RequestStatus newStatus;
        try {
            newStatus = RequestStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Неверный статус: " + status);
        }

        if (newStatus != RequestStatus.CONFIRMED && newStatus != RequestStatus.REJECTED) {
            throw new ValidationException("Можно установить только статусы CONFIRMED или REJECTED");
        }

        int confirmedRequests = requestRepository.countConfirmedRequestsByEventId(eventId);
        int limit = event.getParticipantLimit();

        List<ParticipationRequest> requests = requestRepository.findAllById(requestIds);

        if (requests.isEmpty()) {
            throw new NotFoundException("Заявки не найдены");
        }

        requests.forEach(request -> {
            if (!request.getEvent().getId().equals(eventId)) {
                throw new NotFoundException("Запрос не относится к указанному событию");
            }

            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Статус можно изменить только у заявок, находящихся в состоянии ожидания");
            }
        });

        for (ParticipationRequest request : requests) {
            if (newStatus == RequestStatus.CONFIRMED) {
                if (limit != 0 && confirmedRequests >= limit) {
                    throw new ConflictException("Достигнут лимит по заявкам на данное событие");
                }
                request.setStatus(RequestStatus.CONFIRMED);
                confirmedRequests++;

                event.setConfirmedRequests(confirmedRequests);
                eventRepository.save(event);
            } else if (newStatus == RequestStatus.REJECTED) {
                request.setStatus(RequestStatus.REJECTED);
            }
        }

        List<ParticipationRequest> updatedRequests = requestRepository.saveAll(requests);
        log.info("Updated {} requests for event id: {}", updatedRequests.size(), eventId);

        return updatedRequests.stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }
}
