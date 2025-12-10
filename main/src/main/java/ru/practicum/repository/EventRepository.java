package ru.practicum.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.model.Event;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByInitiatorId(Long userId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);

    List<Event> findByIdIn(List<Long> eventIds);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.category.id = :categoryId")
    Long countByCategoryId(@Param("categoryId") Long categoryId);

    @Query(value = "SELECT * FROM events e " +
            "WHERE (:usersStr IS NULL OR e.initiator_id IN (:users)) " +
            "AND (:statesStr IS NULL OR e.state IN (:states)) " +
            "AND (:categoriesStr IS NULL OR e.category_id IN (:categories)) " +
            "AND (:rangeStartStr IS NULL OR e.event_date >= TO_TIMESTAMP(:rangeStartStr, 'YYYY-MM-DD HH24:MI:SS')) " +
            "AND (:rangeEndStr IS NULL OR e.event_date <= TO_TIMESTAMP(:rangeEndStr, 'YYYY-MM-DD HH24:MI:SS')) " +
            "ORDER BY e.id ASC " +
            "LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<Event> findEventsByAdminNative(
            @Param("users") List<Long> users,
            @Param("usersStr") String usersStr,
            @Param("states") List<String> states,
            @Param("statesStr") String statesStr,
            @Param("categories") List<Long> categories,
            @Param("categoriesStr") String categoriesStr,
            @Param("rangeStartStr") String rangeStartStr,
            @Param("rangeEndStr") String rangeEndStr,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Query(value = "SELECT * FROM events e " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND (:text IS NULL OR LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) " +
            "OR LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%'))) " +
            "AND (:categoriesStr IS NULL OR e.category_id IN (:categories)) " +
            "AND (:paid IS NULL OR e.paid = :paid) " +
            "AND (:rangeStartStr IS NULL OR e.event_date >= TO_TIMESTAMP(:rangeStartStr, 'YYYY-MM-DD HH24:MI:SS')) " +
            "AND (:rangeEndStr IS NULL OR e.event_date <= TO_TIMESTAMP(:rangeEndStr, 'YYYY-MM-DD HH24:MI:SS')) " +
            "ORDER BY e.event_date ASC " +
            "LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<Event> findEventsByPublicNative(
            @Param("text") String text,
            @Param("categories") List<Long> categories,
            @Param("categoriesStr") String categoriesStr,
            @Param("paid") Boolean paid,
            @Param("rangeStartStr") String rangeStartStr,
            @Param("rangeEndStr") String rangeEndStr,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Query("SELECT COUNT(r) FROM ParticipationRequest r WHERE r.event.id = :eventId AND r.status = 'CONFIRMED'")
    Integer countConfirmedRequests(@Param("eventId") Long eventId);

    @Modifying
    @Query(value = "UPDATE events SET views = COALESCE(views, 0) + 1 WHERE id = :eventId", nativeQuery = true)
    void incrementViews(@Param("eventId") Long eventId);
}