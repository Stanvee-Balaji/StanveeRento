package com.example.demo.repository;

import com.example.demo.entity.RentalBookingConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RentalBookingConfigRepository extends JpaRepository<RentalBookingConfigEntity, UUID> {
    Optional<RentalBookingConfigEntity> findFirstByActiveTrueOrderByCreatedAtDesc();
}
