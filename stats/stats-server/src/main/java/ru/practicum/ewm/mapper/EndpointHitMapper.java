package ru.practicum.ewm.mapper;

import org.mapstruct.*;
import ru.practicum.ewm.dto.EndpointHitDto;
import ru.practicum.ewm.model.EndpointHit;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EndpointHitMapper {

    EndpointHitDto toDto(EndpointHit endpointHit);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    EndpointHit toEntity(EndpointHitDto endpointHitDto);
}