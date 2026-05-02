package com.app.aml.security.repository;



import com.app.aml.security.entity.PlatformUserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlatformUserSessionRepository extends JpaRepository<PlatformUserSession, UUID> {

    boolean existsByJwtJtiAndIsRevokedTrue(String jwtJti);

    @Modifying
    @Query("UPDATE PlatformUserSession s SET s.isRevoked = true, s.revokedAt = ?2 WHERE s.jwtJti = ?1")
    void revokeSessionByJti(String jwtJti, Instant revokedAt);
}