package com.wificaptive.sessions.repository;

import com.wificaptive.sessions.entity.GuestSession;
import com.wificaptive.sessions.entity.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface GuestSessionRepository extends JpaRepository<GuestSession, Long> {

    Optional<GuestSession> findBySessionToken(String sessionToken);

    @Query("SELECT s FROM GuestSession s WHERE s.macAddress = :mac AND s.status = 'AUTHORIZED' AND s.expiresAt > :now")
    List<GuestSession> findActiveSessionsByMac(@Param("mac") String mac, @Param("now") Instant now);

    /** Phiên cổng chào theo danh sách MAC (chuẩn hoá chữ thường), mới nhất trước — dùng gắn tên/liên hệ vào thiết bị khách. */
    @Query("SELECT s FROM GuestSession s LEFT JOIN FETCH s.location WHERE LOWER(s.macAddress) IN :macs ORDER BY s.createdAt DESC")
    List<GuestSession> findByMacLowerInOrderByCreatedAtDesc(@Param("macs") java.util.Collection<String> macs);

    @Query("SELECT s FROM GuestSession s WHERE UPPER(REPLACE(REPLACE(s.macAddress, ':', ''), '-', '')) = :cleanMac AND s.status = 'AUTHORIZED'")
    List<GuestSession> findAuthorizedByCleanMac(@Param("cleanMac") String cleanMac);

    @Query("SELECT s FROM GuestSession s WHERE s.status = :status AND s.expiresAt <= :now")
    List<GuestSession> findSessionsToPassivate(@Param("status") SessionStatus status, @Param("now") Instant now);

    @Query("SELECT s FROM GuestSession s LEFT JOIN FETCH s.location l WHERE s.status = :status")
    List<GuestSession> findByStatusWithLocation(@Param("status") SessionStatus status);

    @Query("SELECT s FROM GuestSession s LEFT JOIN FETCH s.location l WHERE s.status = :status AND s.expiresAt > :now AND s.revokedBy IS NULL ORDER BY s.createdAt DESC")
    List<GuestSession> findUnexpiredByStatusWithLocation(@Param("status") SessionStatus status, @Param("now") Instant now);

    @Query(value = "SELECT s FROM GuestSession s LEFT JOIN FETCH s.location l WHERE " +
           "(:locationId IS NULL OR s.location.id = :locationId) AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:search IS NULL OR LOWER(s.macAddress) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "OR LOWER(s.ipAddress) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "OR LOWER(s.guestName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "OR LOWER(s.guestContact) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))",
           countQuery = "SELECT COUNT(s) FROM GuestSession s WHERE " +
           "(:locationId IS NULL OR s.location.id = :locationId) AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:search IS NULL OR LOWER(s.macAddress) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "OR LOWER(s.ipAddress) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "OR LOWER(s.guestName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "OR LOWER(s.guestContact) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<GuestSession> findFiltered(
            @Param("locationId") Long locationId,
            @Param("status") SessionStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    @Query(value = "SELECT s FROM GuestSession s LEFT JOIN FETCH s.location l",
           countQuery = "SELECT COUNT(s) FROM GuestSession s")
    Page<GuestSession> findAllWithLocation(Pageable pageable);

    long countByStatus(SessionStatus status);

    long countByLocationIdAndStatus(Long locationId, SessionStatus status);

    @Query("SELECT COUNT(s) FROM GuestSession s WHERE s.location.id = :locationId AND s.status = 'AUTHORIZED' AND s.expiresAt > :now")
    long countActiveSessionsByLocation(@Param("locationId") Long locationId, @Param("now") Instant now);

    @Query("SELECT COUNT(s) FROM GuestSession s WHERE s.location.id = :locationId AND s.macAddress <> :mac AND s.status = 'AUTHORIZED' AND s.expiresAt > :now")
    long countActiveSessionsByLocationExcludingMac(@Param("locationId") Long locationId, @Param("mac") String mac, @Param("now") Instant now);

    @Query("SELECT s.location.id, COUNT(s) FROM GuestSession s WHERE s.status = 'AUTHORIZED' AND s.expiresAt > :now GROUP BY s.location.id")
    List<Object[]> countActiveSessionsGroupByLocation(@Param("now") Instant now);

    @Query("SELECT COUNT(s) FROM GuestSession s WHERE s.status = 'AUTHORIZED' AND s.expiresAt > :now")
    long countActiveSessions(@Param("now") Instant now);

    @Query("SELECT COUNT(s) FROM GuestSession s WHERE s.createdAt >= :since")
    long countSessionsSince(@Param("since") Instant since);

    @Query("SELECT s.location.name, COUNT(s) FROM GuestSession s GROUP BY s.location.name")
    List<Object[]> countSessionsByLocation();

    @Query("SELECT s.status, COUNT(s) FROM GuestSession s GROUP BY s.status")
    List<Object[]> countSessionsByStatusGroup();

    @Query("SELECT COUNT(s) FROM GuestSession s WHERE s.status <> com.wificaptive.sessions.entity.SessionStatus.PENDING")
    long countSessionsNotPending();

    @Query("SELECT COUNT(s) FROM GuestSession s WHERE s.status IN (com.wificaptive.sessions.entity.SessionStatus.AUTHORIZED, com.wificaptive.sessions.entity.SessionStatus.EXPIRED, com.wificaptive.sessions.entity.SessionStatus.REVOKED, com.wificaptive.sessions.entity.SessionStatus.FAILED)")
    long countSessionsWithOtp();

    @Query("SELECT COUNT(s) FROM GuestSession s WHERE s.status IN (com.wificaptive.sessions.entity.SessionStatus.AUTHORIZED, com.wificaptive.sessions.entity.SessionStatus.EXPIRED, com.wificaptive.sessions.entity.SessionStatus.REVOKED)")
    long countSessionsAuthenticated();

    @Query("SELECT COALESCE(SUM(s.bytesDownloaded + s.bytesUploaded), 0) FROM GuestSession s")
    Long sumTotalBytes();

    @Query("SELECT s.location.id, COUNT(s), COALESCE(SUM(s.bytesDownloaded + s.bytesUploaded), 0) FROM GuestSession s WHERE s.location.id IS NOT NULL GROUP BY s.location.id")
    List<Object[]> sumSessionStatsGroupByLocation();

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant start, Instant end);

    @Query("SELECT COALESCE(SUM(s.bytesDownloaded + s.bytesUploaded), 0) FROM GuestSession s WHERE s.createdAt >= :start AND s.createdAt < :end")
    Long sumTotalBytesBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT s.location.id, COUNT(s), COALESCE(SUM(s.bytesDownloaded + s.bytesUploaded), 0) FROM GuestSession s WHERE s.location.id IS NOT NULL AND s.createdAt >= :start AND s.createdAt < :end GROUP BY s.location.id")
    List<Object[]> sumSessionStatsGroupByLocationBetween(@Param("start") Instant start, @Param("end") Instant end);
}
