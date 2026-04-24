package com.app.aml.feature.ingestion.repository;

import com.app.aml.feature.ingestion.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, UUID> {
    boolean existsByAccountNumber(String accNum);
}