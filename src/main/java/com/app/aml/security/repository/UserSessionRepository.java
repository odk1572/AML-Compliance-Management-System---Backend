package com.app.aml.security.repository;

import com.app.aml.security.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {


    boolean existsByJwtJtiAndIsRevokedTrue(String jwtJti);

    

    List<UserSession> findAllByUserIdAndIsRevokedFalse(UUID userId);

    @Modifying
    @Query("UPDATE UserSession s SET s.isRevoked = true WHERE s.userId = :userId AND s.isRevoked = false")
    void revokeAllActiveSessions(@Param("userId") UUID userId);

    Optional<UserSession> findByJwtJti(String jwtJti);

    @Modifying
    @Query("UPDATE UserSession s SET s.isRevoked = true, s.revokedAt = ?2 WHERE s.jwtJti = ?1")
    void revokeSessionByJti(String jwtJti, Instant revokedAt);

    // 1. Used by TenantSchemaDeactivator (Revokes everyone in the current schema)
    @Modifying
    @Query("UPDATE UserSession s SET s.isRevoked = true WHERE s.isRevoked = false")
    int revokeAllActiveSessions();

    @Modifying
    @Query("UPDATE UserSession s SET s.isRevoked = true WHERE s.userId = :userId AND s.isRevoked = false")
    void revokeAllUserSessions(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE UserSession s SET s.isRevoked = true WHERE s.jwtJti = :jti")
    void revokeSessionByJti(@Param("jti") String jti);
}