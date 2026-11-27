package com.lgb.factoryflow.domain.item.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ItemType {
    RAW_MATERIAL("원재료"),
    FINISHED_GOOD("완제품");

    private final String description;
}
