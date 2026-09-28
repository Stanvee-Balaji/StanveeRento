package com.example.demo.repository;

import com.example.demo.entity.StanveeUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StanveeUserRepository extends JpaRepository<StanveeUser, String> {
    Optional<StanveeUser> findByUsername(String username);
    boolean existsByUsername(String username);
}
