package com.example.demo.repository;

import com.example.demo.entity.AddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<AddressEntity, UUID> {

    List<AddressEntity> findByUsernameOrderByDefaultAddressDescCreatedAtDesc(String username);

    Optional<AddressEntity> findByIdAndUsername(UUID id, String username);

    Optional<AddressEntity> findFirstByUsernameOrderByCreatedAtDesc(String username);

    long countByUsername(String username);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update AddressEntity a set a.defaultAddress = false " +
           "where a.username = :username and a.defaultAddress = true")
    int clearDefault(@Param("username") String username);
}
