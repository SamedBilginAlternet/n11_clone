package com.example.order.entity;

public enum OrderStatus {
    PENDING,     // order created, awaiting payment
    PAID,        // payment succeeded, order confirmed
    CANCELLED,   // payment failed or user cancelled
    FAILED       // unexpected failure during saga
}
