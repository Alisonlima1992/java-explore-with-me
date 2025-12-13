package ru.practicum.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.service.CategoryService;
import ru.practicum.service.CommentService;
import ru.practicum.service.CompilationService;
import ru.practicum.service.EventService;
import ru.practicum.service.StatsIntegrationService;
import ru.practicum.util.Constants;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class PublicController {

    private final CategoryService categoryService;
    private final EventService eventService;
    private final CompilationService compilationService;
    private final CommentService commentService;
    private final StatsIntegrationService statsIntegrationService;

    @GetMapping("/categories")
    public List<CategoryDto> getCategories(
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE_FROM) @Min(0) Integer from,
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE_SIZE) @Positive Integer size) {
        log.info("GET /categories - получение категорий");
        return categoryService.getCategories(from, size);
    }

    @GetMapping("/categories/{catId}")
    public CategoryDto getCategory(@PathVariable Long catId) {
        log.info("GET /categories/{} - получение категории", catId);
        return categoryService.getCategoryById(catId);
    }

    @GetMapping("/events")
    public List<EventShortDto> getEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(pattern = Constants.DATE_TIME_FORMAT) LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = Constants.DATE_TIME_FORMAT) LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE_FROM) @Min(0) Integer from,
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE_SIZE) @Positive Integer size,
            HttpServletRequest request) {
        log.info("GET /events - получение событий с фильтрами");

        statsIntegrationService.saveHit(request.getRequestURI(), request.getRemoteAddr());

        return eventService.getEventsByPublic(text, categories, paid, rangeStart, rangeEnd,
                onlyAvailable, sort, from, size);
    }

    @GetMapping("/events/{id}")
    public EventFullDto getEvent(@PathVariable Long id, HttpServletRequest request) {
        log.info("GET /events/{} - получение события", id);

        statsIntegrationService.saveHit(request.getRequestURI(), request.getRemoteAddr());

        return eventService.getEventByPublic(id);
    }

    @GetMapping("/compilations")
    public List<CompilationDto> getCompilations(
            @RequestParam(required = false) Boolean pinned,
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE_FROM) @Min(0) Integer from,
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE_SIZE) @Positive Integer size,
            HttpServletRequest request) {
        log.info("GET /compilations - получение подборок");

        statsIntegrationService.saveHit(request.getRequestURI(), request.getRemoteAddr());

        return compilationService.getCompilations(pinned, from, size);
    }

    @GetMapping("/compilations/{compId}")
    public CompilationDto getCompilation(@PathVariable Long compId, HttpServletRequest request) {
        log.info("GET /compilations/{} - получение подборки", compId);

        statsIntegrationService.saveHit(request.getRequestURI(), request.getRemoteAddr());

        return compilationService.getCompilationById(compId);
    }

    @GetMapping("/events/{eventId}/comments")
    public List<CommentDto> getEventComments(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE_FROM) @Min(0) Integer from,
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE_SIZE) @Positive Integer size,
            HttpServletRequest request) {
        log.info("GET /events/{}/comments - получение комментариев события", eventId);

        statsIntegrationService.saveHit(request.getRequestURI(), request.getRemoteAddr());

        return commentService.getEventComments(eventId, from, size);
    }

    @GetMapping("/comments/{commentId}")
    public CommentDto getComment(@PathVariable Long commentId, HttpServletRequest request) {
        log.info("GET /comments/{} - получение комментария", commentId);

        statsIntegrationService.saveHit(request.getRequestURI(), request.getRemoteAddr());

        return commentService.getCommentById(commentId);
    }
}