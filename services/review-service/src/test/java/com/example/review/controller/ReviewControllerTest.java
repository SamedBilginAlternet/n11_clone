package com.example.review.controller;

import com.example.review.dto.CreateReviewRequest;
import com.example.review.entity.Review;
import com.example.review.repository.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock ReviewRepository repo;

    @InjectMocks ReviewController controller;

    private Review existing(long productId, String email) {
        return Review.builder()
                .id(1L).productId(productId).userEmail(email).userName("X")
                .rating(5).comment("ok").createdAt(Instant.now()).build();
    }

    // ────────────────────────────────────────────────────────────────────
    // create — uniqueness + display name
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create rejects a second review for the same product by the same user")
    void create_rejectsDuplicate() {
        when(repo.findByUserEmailOrderByIdDesc("u@example.com"))
                .thenReturn(List.of(existing(10L, "u@example.com")));

        assertThatThrownBy(() -> controller.create(
                "u@example.com", new CreateReviewRequest(10L, 4, "yine iyi")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("zaten yorum");
    }

    @Test
    @DisplayName("create derives display name from email local-part, capitalized")
    void create_deriveDisplayNameFromEmail() {
        when(repo.findByUserEmailOrderByIdDesc("ayse@n11demo.com")).thenReturn(List.of());
        when(repo.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        controller.create("ayse@n11demo.com", new CreateReviewRequest(10L, 5, "harika"));

        ArgumentCaptor<Review> saved = ArgumentCaptor.forClass(Review.class);
        org.mockito.Mockito.verify(repo).save(saved.capture());
        assertThat(saved.getValue().getUserName()).isEqualTo("Ayse");
    }

    // ────────────────────────────────────────────────────────────────────
    // delete — scoped to owner
    // ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete refuses to remove a review owned by someone else (404, not 403, to avoid leaking existence)")
    void delete_foreignReview_throws404() {
        when(repo.findByIdAndUserEmail(1L, "mallory@x.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.delete("mallory@x.com", 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Yorum bulunamadı");
    }

    @Test
    @DisplayName("delete removes the review when owned by the caller")
    void delete_ownReview_succeeds() {
        Review own = existing(10L, "u@example.com");
        when(repo.findByIdAndUserEmail(1L, "u@example.com")).thenReturn(Optional.of(own));

        var response = controller.delete("u@example.com", 1L);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        org.mockito.Mockito.verify(repo).delete(own);
    }
}
