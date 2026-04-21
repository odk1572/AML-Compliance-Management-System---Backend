package com.app.aml.feature.platformuser.repository;

import com.app.aml.feature.platformuser.entity.PlatformUserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface PlatformUserSessionRepository extends JpaRepository<PlatformUserSession, UUID> {
}