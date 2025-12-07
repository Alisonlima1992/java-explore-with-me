package ru.practicum.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.util.Constants;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipationRequestDto {

    private Long id;
    private Long event;
    private Long requester;

    @JsonFormat(pattern = Constants.DATE_TIME_FORMAT)
    private LocalDateTime created;

    private ParticipationRequest.RequestStatus status;
}