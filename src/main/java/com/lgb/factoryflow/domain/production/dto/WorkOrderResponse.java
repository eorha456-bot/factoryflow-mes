package com.lgb.factoryflow.domain.production.dto;

import com.lgb.factoryflow.domain.production.entity.WorkOrder;
import com.lgb.factoryflow.domain.production.entity.WorkOrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class WorkOrderResponse {

    private final Long id;
    private final String orderNumber;
    private final Long itemId;
    private final String itemCode;
    private final String itemName;
    private final String unit;
    private final int targetQuantity;
    private final int producedGoodQuantity;
    private final int producedDefectQuantity;
    private final double progressRate;
    private final WorkOrderStatus status;
    private final String statusDescription;
    private final LocalDate plannedDate;
    private final String remark;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static WorkOrderResponse from(WorkOrder workOrder) {
        return WorkOrderResponse.builder()
                .id(workOrder.getId())
                .orderNumber(workOrder.getOrderNumber())
                .itemId(workOrder.getItem().getId())
                .itemCode(workOrder.getItem().getItemCode())
                .itemName(workOrder.getItem().getItemName())
                .unit(workOrder.getItem().getUnit())
                .targetQuantity(workOrder.getTargetQuantity())
                .producedGoodQuantity(workOrder.getProducedGoodQuantity())
                .producedDefectQuantity(workOrder.getProducedDefectQuantity())
                .progressRate(workOrder.calculateProgressRate())
                .status(workOrder.getStatus())
                .statusDescription(workOrder.getStatus().getDescription())
                .plannedDate(workOrder.getPlannedDate())
                .remark(workOrder.getRemark())
                .createdAt(workOrder.getCreatedAt())
                .updatedAt(workOrder.getUpdatedAt())
                .build();
    }
}
