package ru.practicum.dto.comment;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.util.Constants;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDto {

    private Long id;
    private String text;
    private UserShortDto author;
    private Long eventId;

    @JsonFormat(pattern = Constants.DATE_TIME_FORMAT)
    private LocalDateTime created;

    @JsonFormat(pattern = Constants.DATE_TIME_FORMAT)
    private LocalDateTime updated;
}