package com.lgb.factoryflow.domain.production.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorkOrderStatus {
    WAITING("대기"),
    IN_PROGRESS("작업중"),
    COMPLETED("완료"),
    CANCELED("취소");

    private final String description;
}
