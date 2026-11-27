package com.lgb.factoryflow.domain.production.dto;

import com.lgb.factoryflow.domain.production.entity.ProductionResult;
import com.lgb.factoryflow.domain.production.entity.WorkOrder;
import com.lgb.factoryflow.domain.production.entity.WorkOrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProductionResultResponse {

    private final Long id;
    private final Long workOrderId;
    private final String orderNumber;
    private final Long itemId;
    private final String itemCode;
    private final String itemName;
    private final String unit;
    private final int goodQuantity;
    private final int defectQuantity;
    private final int totalQuantity;
    private final int cumulativeGoodQuantity;
    private final int cumulativeDefectQuantity;
    private final WorkOrderStatus workOrderStatus;
    private final String workerName;
    private final String remark;
    private final LocalDateTime createdAt;

    public static ProductionResultResponse from(ProductionResult result) {
        WorkOrder workOrder = result.getWorkOrder();
        return ProductionResultResponse.builder()
                .id(result.getId())
                .workOrderId(workOrder.getId())
                .orderNumber(workOrder.getOrderNumber())
                .itemId(workOrder.getItem().getId())
                .itemCode(workOrder.getItem().getItemCode())
                .itemName(workOrder.getItem().getItemName())
                .unit(workOrder.getItem().getUnit())
                .goodQuantity(result.getGoodQuantity())
                .defectQuantity(result.getDefectQuantity())
                .totalQuantity(result.getTotalQuantity())
                .cumulativeGoodQuantity(workOrder.getProducedGoodQuantity())
                .cumulativeDefectQuantity(workOrder.getProducedDefectQuantity())
                .workOrderStatus(workOrder.getStatus())
                .workerName(result.getWorkerName())
                .remark(result.getRemark())
                .createdAt(result.getCreatedAt())
                .build();
    }
}
