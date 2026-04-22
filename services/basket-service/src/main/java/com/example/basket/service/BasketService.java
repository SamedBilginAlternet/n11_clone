package com.example.basket.service;

import com.example.basket.dto.AddItemRequest;
import com.example.basket.dto.BasketResponse;
import com.example.basket.dto.UpdateItemRequest;
import com.example.basket.entity.Basket;
import com.example.basket.entity.BasketItem;
import com.example.basket.exception.ResourceNotFoundException;
import com.example.basket.repository.BasketItemRepository;
import com.example.basket.repository.BasketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BasketService {

    private final BasketRepository basketRepository;
    private final BasketItemRepository basketItemRepository;

    @Transactional(readOnly = true)
    public BasketResponse getBasket(String userEmail) {
        return BasketResponse.from(loadOrCreate(userEmail));
    }

    @Transactional
    public BasketResponse addItem(String userEmail, AddItemRequest req) {
        Basket basket = loadOrCreate(userEmail);

        // Merge if same product already exists — increment quantity instead of duplicating.
        basket.getItems().stream()
                .filter(i -> i.getProductId().equals(req.productId()))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.setQuantity(existing.getQuantity() + req.quantity()),
                        () -> basket.getItems().add(BasketItem.builder()
                                .basket(basket)
                                .productId(req.productId())
                                .productName(req.productName())
                                .productPrice(req.productPrice())
                                .imageUrl(req.imageUrl())
                                .quantity(req.quantity())
                                .build())
                );

        basketRepository.save(basket);
        return BasketResponse.from(basket);
    }

    @Transactional
    public BasketResponse updateItem(String userEmail, Long itemId, UpdateItemRequest req) {
        Basket basket = loadOrCreate(userEmail);
        BasketItem item = basket.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Sepet öğesi", itemId));
        item.setQuantity(req.quantity());
        basketRepository.save(basket);
        return BasketResponse.from(basket);
    }

    @Transactional
    public BasketResponse removeItem(String userEmail, Long itemId) {
        Basket basket = loadOrCreate(userEmail);
        boolean removed = basket.getItems().removeIf(i -> i.getId().equals(itemId));
        if (!removed) {
            throw new ResourceNotFoundException("Sepet öğesi", itemId);
        }
        basketRepository.save(basket);
        return BasketResponse.from(basket);
    }

    @Transactional
    public void clear(String userEmail) {
        basketRepository.findByUserEmail(userEmail).ifPresent(b -> {
            b.getItems().clear();
            basketRepository.save(b);
        });
    }

    @Transactional
    public Basket createEmptyBasketFor(String userEmail) {
        if (basketRepository.existsByUserEmail(userEmail)) {
            return basketRepository.findByUserEmail(userEmail).orElseThrow();
        }
        return basketRepository.save(Basket.builder().userEmail(userEmail).build());
    }

    @Transactional
    public void deleteBasketFor(String userEmail) {
        basketRepository.deleteByUserEmail(userEmail);
    }

    private Basket loadOrCreate(String userEmail) {
        return basketRepository.findByUserEmail(userEmail)
                .orElseGet(() -> basketRepository.save(Basket.builder().userEmail(userEmail).build()));
    }
}
