package com.lgb.factoryflow.domain.partner.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PartnerType {
    SUPPLIER("매입처"),
    CUSTOMER("납품처"),
    BOTH("매입·납품 겸용");

    private final String description;
}
