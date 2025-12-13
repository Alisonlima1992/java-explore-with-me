package ru.practicum.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import ru.practicum.model.ParticipationRequest;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRequestStatusUpdateRequest {

    @NotEmpty(message = "Список ID заявок не может быть пустым")
    private List<Long> requestIds;

    @NotNull(message = "Статус не может быть пустым")
    private ParticipationRequest.RequestStatus status;
}