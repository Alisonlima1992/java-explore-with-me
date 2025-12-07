package ru.practicum.mapper;

import org.mapstruct.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.model.Event;

import java.time.LocalDateTime;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {CategoryMapper.class, UserMapper.class})
public interface EventMapper {

    @Mapping(target = "state", expression = "java(event.getState().name())")
    @Mapping(target = "location", expression = "java(mapToLocationDto(event))")
    EventFullDto toFullDto(Event event);

    @Mapping(target = "confirmedRequests", source = "confirmedRequests")
    @Mapping(target = "views", source = "views")
    EventShortDto toShortDto(Event event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "initiator", ignore = true)
    @Mapping(target = "state", constant = "PENDING")
    @Mapping(target = "createdOn", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "confirmedRequests", constant = "0")
    @Mapping(target = "views", constant = "0L")
    @Mapping(target = "locationLat", source = "location.lat")
    @Mapping(target = "locationLon", source = "location.lon")
    Event toEntity(NewEventDto newEventDto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "initiator", ignore = true)
    void updateEventFromDto(NewEventDto dto, @MappingTarget Event entity);

    default EventFullDto.LocationDto mapToLocationDto(Event event) {
        if (event.getLocationLat() == null || event.getLocationLon() == null) {
            return null;
        }
        return EventFullDto.LocationDto.builder()
                .lat(event.getLocationLat())
                .lon(event.getLocationLon())
                .build();
    }
}