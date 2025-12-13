package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.model.Comment;
import ru.practicum.model.Event;
import ru.practicum.model.Event.EventState;
import ru.practicum.model.User;
import ru.practicum.repository.CommentRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        log.info("Creating comment for event id: {} by user id: {}", eventId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь", userId));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие", eventId));

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя комментировать неопубликованное событие");
        }

        Comment comment = commentMapper.toEntity(newCommentDto);
        comment.setEvent(event);
        comment.setAuthor(user);

        Comment savedComment = commentRepository.save(comment);
        log.info("Comment created with id: {}", savedComment.getId());

        return commentMapper.toDto(savedComment);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, NewCommentDto updateCommentDto) {
        log.info("Updating comment id: {} by user id: {}", commentId, userId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий", commentId));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new NotFoundException("Комментарий не принадлежит пользователю");
        }

        if (comment.getCreated().plusHours(24).isBefore(LocalDateTime.now())) {
            throw new ValidationException("Редактирование комментария возможно только в течение 24 часов после создания");
        }

        comment.setText(updateCommentDto.getText());
        comment.setUpdated(LocalDateTime.now());

        Comment updatedComment = commentRepository.save(comment);
        log.info("Comment id: {} updated", commentId);

        return commentMapper.toDto(updatedComment);
    }

    @Override
    @Transactional
    public void deleteCommentByUser(Long userId, Long commentId) {
        log.info("Deleting comment id: {} by user id: {}", commentId, userId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий", commentId));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new NotFoundException("Комментарий не принадлежит пользователю");
        }

        commentRepository.delete(comment);
        log.info("Comment id: {} deleted by user", commentId);
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        log.info("Deleting comment id: {} by admin", commentId);

        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException("Комментарий", commentId);
        }

        commentRepository.deleteById(commentId);
        log.info("Comment id: {} deleted by admin", commentId);
    }

    @Override
    public List<CommentDto> getEventComments(Long eventId, Integer from, Integer size) {
        log.info("Getting comments for event id: {}, from: {}, size: {}", eventId, from, size);

        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Событие", eventId);
        }

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("created").descending());

        return commentRepository.findByEventId(eventId, pageable).getContent()
                .stream()
                .map(commentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> getUserComments(Long userId, Integer from, Integer size) {
        log.info("Getting comments for user id: {}, from: {}, size: {}", userId, from, size);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь", userId);
        }

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("created").descending());

        return commentRepository.findByAuthorId(userId, pageable).getContent()
                .stream()
                .map(commentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CommentDto getCommentById(Long commentId) {
        log.info("Getting comment by id: {}", commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий", commentId));

        return commentMapper.toDto(comment);
    }
}