package ru.practicum.ewm.exceptions;

import java.util.Map;

public record ErrorResponse(String error, Map<String, String> descriptions) {
}