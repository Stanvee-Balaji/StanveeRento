package com.example.demo.repository;

import com.example.demo.entity.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItemEntity, UUID> {

    List<CartItemEntity> findByCart_IdOrderByCreatedAtAsc(UUID cartId);

    Optional<CartItemEntity> findByIdAndCart_Id(UUID id, UUID cartId);

    long countByCart_Id(UUID cartId);

    void deleteByCart_Id(UUID cartId);

    Optional<CartItemEntity> findByCart_IdAndProduct_IdAndSizeIgnoreCaseAndStartDateAndEndDate(
            UUID cartId, UUID productId, String size, LocalDate startDate, LocalDate endDate);
}
