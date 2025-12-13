package ru.practicum.service;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;

import java.util.List;

public interface CommentService {

    CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto);

    CommentDto updateComment(Long userId, Long commentId, NewCommentDto updateCommentDto);

    void deleteCommentByUser(Long userId, Long commentId);

    void deleteCommentByAdmin(Long commentId);

    List<CommentDto> getEventComments(Long eventId, @Min(0) Integer from, @Positive Integer size);

    List<CommentDto> getUserComments(Long userId, @Min(0) Integer from, @Positive Integer size);

    CommentDto getCommentById(Long commentId);
}