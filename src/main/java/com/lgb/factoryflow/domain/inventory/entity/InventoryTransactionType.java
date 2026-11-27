package com.lgb.factoryflow.domain.inventory.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InventoryTransactionType {
    INBOUND("입고"),
    OUTBOUND("출고"),
    PRODUCTION_IN("생산입고"),
    PRODUCTION_OUT("생산투입"),
    ADJUST("재고조정");

    private final String description;
}
