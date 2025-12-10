package ru.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.dto.event.UpdateEventRequest;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.exception.ValidationException;
import ru.practicum.service.CommentService;
import ru.practicum.service.EventService;
import ru.practicum.service.RequestService;
import ru.practicum.util.Constants;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
@Slf4j
public class PrivateController {

    private final EventService eventService;
    private final RequestService requestService;
    private final CommentService commentService;

    @GetMapping("/events")
    public List<EventShortDto> getUserEvents(@PathVariable Long userId,
                                             @RequestParam(defaultValue = Constants.DEFAULT_PAGE_FROM) Integer from,
                                             @RequestParam(defaultValue = Constants.DEFAULT_PAGE_SIZE) Integer size) {
        log.info("GET /users/{}/events - получение событий пользователя", userId);
        return eventService.getUserEvents(userId, from, size);
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto createEvent(@PathVariable Long userId,
                                    @Valid @RequestBody NewEventDto newEventDto) {
        log.info("POST /users/{}/events - создание события", userId);
        return eventService.createEvent(userId, newEventDto);
    }

    @GetMapping("/events/{eventId}")
    public EventFullDto getUserEvent(@PathVariable Long userId,
                                     @PathVariable Long eventId) {
        log.info("GET /users/{}/events/{} - получение события пользователя", userId, eventId);
        return eventService.getUserEventById(userId, eventId);
    }

    @PatchMapping("/events/{eventId}")
    public EventFullDto updateEvent(@PathVariable Long userId,
                                    @PathVariable Long eventId,
                                    @Valid @RequestBody UpdateEventRequest updateEventRequest) {
        log.info("PATCH /users/{}/events/{} - обновление события пользователем", userId, eventId);
        return eventService.updateEventByUser(userId, eventId, updateEventRequest);
    }

    @GetMapping("/requests")
    public List<ParticipationRequestDto> getUserRequests(@PathVariable Long userId) {
        log.info("GET /users/{}/requests - получение запросов пользователя", userId);
        return requestService.getUserRequests(userId);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createRequest(@PathVariable Long userId,
                                                 @RequestParam(required = true) Long eventId) {
        log.info("POST /users/{}/requests - создание запроса на участие", userId);
        return requestService.createRequest(userId, eventId);
    }

    @PatchMapping("/requests/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable Long userId,
                                                 @PathVariable Long requestId) {
        log.info("PATCH /users/{}/requests/{}/cancel - отмена запроса", userId, requestId);
        return requestService.cancelRequest(userId, requestId);
    }

    @GetMapping("/events/{eventId}/requests")
    public List<ParticipationRequestDto> getEventRequests(@PathVariable Long userId,
                                                          @PathVariable Long eventId) {
        log.info("GET /users/{}/events/{}/requests - получение запросов на участие в событии", userId, eventId);
        return requestService.getEventRequests(userId, eventId);
    }

    @PatchMapping("/events/{eventId}/requests")
    public List<ParticipationRequestDto> updateRequestStatuses(@PathVariable Long userId,
                                                               @PathVariable Long eventId,
                                                               @RequestBody Map<String, Object> updates) {
        log.info("PATCH /users/{}/events/{}/requests - обновление статусов запросов", userId, eventId);

        @SuppressWarnings("unchecked")
        List<Long> requestIds = (List<Long>) updates.get("requestIds");
        String status = (String) updates.get("status");

        if (requestIds == null || requestIds.isEmpty()) {
            throw new ValidationException("Список ID заявок не может быть пустым");
        }

        if (status == null || status.isBlank()) {
            throw new ValidationException("Статус не может быть пустым");
        }

        return requestService.updateRequestStatuses(userId, eventId, requestIds, status);
    }


    @PostMapping("/events/{eventId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(@PathVariable Long userId,
                                    @PathVariable Long eventId,
                                    @Valid @RequestBody NewCommentDto newCommentDto) {
        log.info("POST /users/{}/events/{}/comments - создание комментария", userId, eventId);
        return commentService.createComment(userId, eventId, newCommentDto);
    }

    @PatchMapping("/comments/{commentId}")
    public CommentDto updateComment(@PathVariable Long userId,
                                    @PathVariable Long commentId,
                                    @Valid @RequestBody NewCommentDto updateCommentDto) {
        log.info("PATCH /users/{}/comments/{} - обновление комментария", userId, commentId);
        return commentService.updateComment(userId, commentId, updateCommentDto);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long userId,
                              @PathVariable Long commentId) {
        log.info("DELETE /users/{}/comments/{} - удаление комментария пользователем", userId, commentId);
        commentService.deleteCommentByUser(userId, commentId);
    }

    @GetMapping("/comments")
    public List<CommentDto> getUserComments(@PathVariable Long userId,
                                            @RequestParam(defaultValue = Constants.DEFAULT_PAGE_FROM) Integer from,
                                            @RequestParam(defaultValue = Constants.DEFAULT_PAGE_SIZE) Integer size) {
        log.info("GET /users/{}/comments - получение комментариев пользователя", userId);
        return commentService.getUserComments(userId, from, size);
    }
}