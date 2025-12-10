package ru.practicum.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFoundException(NotFoundException e) {
        return Map.of(
                "status", "NOT_FOUND",
                "reason", "Требуемый объект не найден",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
        );
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidationException(ValidationException e) {
        return Map.of(
                "status", "BAD_REQUEST",
                "reason", "Некорректный запрос",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
        );
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleConflictException(ConflictException e) {
        return Map.of(
                "status", "CONFLICT",
                "reason", "Нарушение целостности данных",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        return Map.of(
                "status", "BAD_REQUEST",
                "reason", "Некорректные параметры запроса",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        return Map.of(
                "status", "BAD_REQUEST",
                "reason", "Некорректный тип параметра",
                "message", String.format("Параметр '%s' имеет неверный тип", e.getName()),
                "timestamp", LocalDateTime.now()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleException(Exception e) {
        return Map.of(
                "status", "INTERNAL_SERVER_ERROR",
                "reason", "Внутренняя ошибка сервера",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        return Map.of(
                "status", "BAD_REQUEST",
                "reason", "Отсутствует обязательный параметр запроса",
                "message", String.format("Параметр '%s' обязателен", e.getParameterName()),
                "timestamp", LocalDateTime.now()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        String message = "Неверный формат JSON";

        if (e.getCause() instanceof InvalidFormatException) {
            InvalidFormatException ife = (InvalidFormatException) e.getCause();
            message = String.format("Неверный тип данных для поля '%s'. Ожидается: %s",
                    ife.getPath().get(ife.getPath().size() - 1).getFieldName(),
                    ife.getTargetType().getSimpleName());
        }

        return Map.of(
                "status", "BAD_REQUEST",
                "reason", "Некорректный запрос",
                "message", message,
                "timestamp", LocalDateTime.now()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));

        return Map.of(
                "status", "BAD_REQUEST",
                "reason", "Нарушение ограничений валидации",
                "message", message,
                "timestamp", LocalDateTime.now()
        );
    }
}