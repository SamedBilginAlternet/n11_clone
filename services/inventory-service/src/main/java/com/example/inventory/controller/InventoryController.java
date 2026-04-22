package com.example.inventory.controller;

import com.example.inventory.dto.InventoryResponse;
import com.example.inventory.repository.InventoryItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryItemRepository items;

    @GetMapping
    public List<InventoryResponse> all() {
        return items.findAll().stream().map(InventoryResponse::from).toList();
    }

    @GetMapping("/{productId}")
    public InventoryResponse get(@PathVariable Long productId) {
        return items.findById(productId)
                .map(InventoryResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Ürün envanterde yok: " + productId));
    }
}
