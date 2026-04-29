package com.app.aml.feature.platformuser.repository;

import com.app.aml.feature.platformuser.entity.PlatformUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlatformUserRepository extends JpaRepository<PlatformUser, UUID> {
    Optional<PlatformUser> findByEmail(String email);
    boolean existsByEmail(String adminEmail);
    Optional<PlatformUser> findByUserCode(String code);
}