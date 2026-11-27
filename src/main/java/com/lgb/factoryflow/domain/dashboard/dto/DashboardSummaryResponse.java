package com.lgb.factoryflow.domain.dashboard.dto;

import com.lgb.factoryflow.domain.inventory.dto.InventoryResponse;
import com.lgb.factoryflow.domain.production.dto.WorkOrderResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class DashboardSummaryResponse {

    private final LocalDate baseDate;

    private final long waitingWorkOrderCount;
    private final long inProgressWorkOrderCount;
    private final long pausedWorkOrderCount;
    private final long completedWorkOrderCount;

    private final int todayResultCount;
    private final int todayGoodQuantity;
    private final int todayDefectQuantity;
    private final int todayTotalQuantity;
    private final double todayDefectRate;

    private final int belowSafetyStockCount;
    private final List<InventoryResponse> belowSafetyStockItems;
    private final List<WorkOrderResponse> inProgressWorkOrders;
    private final List<WorkOrderResponse> pausedWorkOrders;
}
