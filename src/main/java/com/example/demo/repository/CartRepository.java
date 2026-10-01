package com.example.demo.repository;

import com.example.demo.entity.CartEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<CartEntity, UUID> {

    Optional<CartEntity> findFirstByUsernameAndStatusOrderByCreatedAtDesc(String username, String status);
}
