package com.example.demo.repository;

import com.example.demo.entity.WishlistItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WishlistItemRepository extends JpaRepository<WishlistItemEntity, UUID> {

    List<WishlistItemEntity> findByUsernameOrderByCreatedAtDesc(String username);

    Optional<WishlistItemEntity> findByUsernameAndProduct_Id(String username, UUID productId);

    boolean existsByUsernameAndProduct_Id(String username, UUID productId);

    long countByUsername(String username);

    void deleteByUsername(String username);
}
