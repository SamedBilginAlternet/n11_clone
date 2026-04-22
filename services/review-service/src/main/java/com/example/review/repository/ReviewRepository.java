package com.example.review.repository;

import com.example.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByProductIdOrderByIdDesc(Long productId, Pageable pageable);

    List<Review> findByUserEmailOrderByIdDesc(String userEmail);

    Optional<Review> findByIdAndUserEmail(Long id, String userEmail);

    @Query("""
           SELECT COUNT(r), COALESCE(AVG(r.rating), 0.0)
           FROM Review r WHERE r.productId = :productId
           """)
    Object[] statsForProduct(Long productId);
}
