package com.example.review.controller;

import com.example.review.dto.CreateReviewRequest;
import com.example.review.dto.ReviewResponse;
import com.example.review.dto.ReviewStatsResponse;
import com.example.review.entity.Review;
import com.example.review.repository.ReviewRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewRepository repo;

    // --- Public ---

    @GetMapping("/product/{productId}")
    public Page<ReviewResponse> listByProduct(
            @PathVariable Long productId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return repo.findByProductIdOrderByIdDesc(productId, pageable).map(ReviewResponse::from);
    }

    @GetMapping("/product/{productId}/stats")
    public ReviewStatsResponse stats(@PathVariable Long productId) {
        Object[] row = repo.statsForProduct(productId);
        Object[] stats = row.length == 1 && row[0] instanceof Object[] arr ? arr : row;
        long count = ((Number) stats[0]).longValue();
        double avg = ((Number) stats[1]).doubleValue();
        return new ReviewStatsResponse(productId, count, avg);
    }

    // --- Authed ---

    @GetMapping("/mine")
    public List<ReviewResponse> mine(@AuthenticationPrincipal String userEmail) {
        return repo.findByUserEmailOrderByIdDesc(userEmail).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(
            @AuthenticationPrincipal String userEmail,
            @Valid @RequestBody CreateReviewRequest req
    ) {
        if (repo.findByUserEmailOrderByIdDesc(userEmail).stream()
                .anyMatch(r -> r.getProductId().equals(req.productId()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu ürüne zaten yorum yaptın.");
        }
        Review saved = repo.save(Review.builder()
                .productId(req.productId())
                .userEmail(userEmail)
                .userName(displayNameFor(userEmail))
                .rating(req.rating())
                .comment(req.comment())
                .build());
        return ReviewResponse.from(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal String userEmail, @PathVariable Long id) {
        Review r = repo.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Yorum bulunamadı."));
        repo.delete(r);
        return ResponseEntity.noContent().build();
    }

    private String displayNameFor(String email) {
        int at = email.indexOf('@');
        String local = at > 0 ? email.substring(0, at) : email;
        return local.substring(0, 1).toUpperCase() + local.substring(1);
    }
}
