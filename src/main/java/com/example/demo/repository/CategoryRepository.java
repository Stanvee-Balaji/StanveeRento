//package com.example.demo.repository;
//
//import com.example.demo.entity.CategoryEntity;
//import org.springframework.data.jpa.repository.JpaRepository;
//import java.util.Optional;
//import java.util.UUID;
//
//public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {
//    Optional<CategoryEntity> findByIdAndDeletedAtIsNull(UUID id);
//    
//    
//
//    /** Duplicate-name guard: check for an existing non-deleted category with the same name. */
//    Optional<CategoryEntity> findByNameIgnoreCaseAndDeletedAtIsNull(String name);
// 
//    /**
//     * Duplicate-name guard during update: same as above but excluding the category
//     * being updated so a category can keep its own name without triggering the check.
//     */
//    Optional<CategoryEntity> findByNameIgnoreCaseAndDeletedAtIsNullAndIdNot(String name, UUID id);
//}


package com.example.demo.repository;

import com.example.demo.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID>,
        JpaSpecificationExecutor<CategoryEntity> {

    /** Used by ProductService to validate that the category exists and is not soft-deleted. */
    Optional<CategoryEntity> findByIdAndDeletedAtIsNull(UUID id);

    /** Duplicate-name guard: check for an existing non-deleted category with the same name. */
    Optional<CategoryEntity> findByNameIgnoreCaseAndDeletedAtIsNull(String name);

    
       List<CategoryEntity> findByDeletedAtIsNullAndActiveTrue();
    
    
    
    
    
    /**
     * Duplicate-name guard during update: same as above but excluding the category
     * being updated so a category can keep its own name without triggering the check.
     */
    Optional<CategoryEntity> findByNameIgnoreCaseAndDeletedAtIsNullAndIdNot(String name, UUID id);
}