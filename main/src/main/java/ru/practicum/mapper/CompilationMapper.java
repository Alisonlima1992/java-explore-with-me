package ru.practicum.mapper;

import org.mapstruct.*;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.model.Compilation;
import ru.practicum.model.Event;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CompilationMapper {

    @Mapping(target = "events", source = "events")
    CompilationDto toDto(Compilation compilation);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    Compilation toEntity(NewCompilationDto newCompilationDto);

    default Set<Long> mapEventsToIds(Set<Event> events) {
        return events.stream()
                .map(Event::getId)
                .collect(Collectors.toSet());
    }

    default Set<Event> mapIdsToEvents(Set<Long> eventIds) {
        if (eventIds == null) {
            return Set.of();
        }
        return eventIds.stream()
                .map(id -> {
                    Event event = new Event();
                    event.setId(id);
                    return event;
                })
                .collect(Collectors.toSet());
    }
}