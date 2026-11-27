package com.lgb.factoryflow.domain.dashboard.service;

import com.lgb.factoryflow.domain.dashboard.dto.DashboardSummaryResponse;
import com.lgb.factoryflow.domain.inventory.dto.InventoryResponse;
import com.lgb.factoryflow.domain.inventory.service.InventoryService;
import com.lgb.factoryflow.domain.production.dto.WorkOrderResponse;
import com.lgb.factoryflow.domain.production.entity.ProductionResult;
import com.lgb.factoryflow.domain.production.entity.WorkOrderStatus;
import com.lgb.factoryflow.domain.production.repository.ProductionResultRepository;
import com.lgb.factoryflow.domain.production.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final WorkOrderRepository workOrderRepository;
    private final ProductionResultRepository productionResultRepository;
    private final InventoryService inventoryService;

    public DashboardSummaryResponse getSummary(LocalDate date) {
        LocalDate baseDate = (date != null) ? date : LocalDate.now();
        LocalDateTime startOfDay = baseDate.atStartOfDay();
        LocalDateTime nextDayStart = baseDate.plusDays(1).atStartOfDay();

        long waitingCount = workOrderRepository.countByStatus(WorkOrderStatus.WAITING);
        List<WorkOrderResponse> inProgressWorkOrders = workOrderRepository
                .findByStatusOrderByIdDesc(WorkOrderStatus.IN_PROGRESS)
                .stream()
                .map(WorkOrderResponse::from)
                .toList();
        long completedCount = workOrderRepository.countByStatus(WorkOrderStatus.COMPLETED);

        List<ProductionResult> dailyResults = productionResultRepository
                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdDesc(startOfDay, nextDayStart);

        int goodQuantity = 0;
        int defectQuantity = 0;
        for (ProductionResult result : dailyResults) {
            goodQuantity += result.getGoodQuantity();
            defectQuantity += result.getDefectQuantity();
        }
        int totalQuantity = goodQuantity + defectQuantity;
        double defectRate = calculateDefectRate(defectQuantity, totalQuantity);

        List<InventoryResponse> belowSafetyStockItems = inventoryService.getInventories(null, true);

        return DashboardSummaryResponse.builder()
                .baseDate(baseDate)
                .waitingWorkOrderCount(waitingCount)
                .inProgressWorkOrderCount(inProgressWorkOrders.size())
                .completedWorkOrderCount(completedCount)
                .todayResultCount(dailyResults.size())
                .todayGoodQuantity(goodQuantity)
                .todayDefectQuantity(defectQuantity)
                .todayTotalQuantity(totalQuantity)
                .todayDefectRate(defectRate)
                .belowSafetyStockCount(belowSafetyStockItems.size())
                .belowSafetyStockItems(belowSafetyStockItems)
                .inProgressWorkOrders(inProgressWorkOrders)
                .build();
    }

    private double calculateDefectRate(int defectQuantity, int totalQuantity) {
        if (totalQuantity <= 0) {
            return 0.0;
        }
        double rate = (defectQuantity * 100.0) / totalQuantity;
        return Math.round(rate * 10.0) / 10.0;
    }
}
