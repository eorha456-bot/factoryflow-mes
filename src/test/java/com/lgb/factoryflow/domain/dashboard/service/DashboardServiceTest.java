package com.lgb.factoryflow.domain.dashboard.service;

import com.lgb.factoryflow.domain.dashboard.dto.DashboardSummaryResponse;
import com.lgb.factoryflow.domain.inventory.dto.InventoryResponse;
import com.lgb.factoryflow.domain.inventory.service.InventoryService;
import com.lgb.factoryflow.domain.item.entity.Item;
import com.lgb.factoryflow.domain.item.entity.ItemType;
import com.lgb.factoryflow.domain.production.entity.ProductionResult;
import com.lgb.factoryflow.domain.production.entity.WorkOrder;
import com.lgb.factoryflow.domain.production.entity.WorkOrderStatus;
import com.lgb.factoryflow.domain.production.repository.ProductionResultRepository;
import com.lgb.factoryflow.domain.production.repository.WorkOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private ProductionResultRepository productionResultRepository;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("지정 날짜의 작업지시 건수, 생산량/불량률, 안전재고 미만 품목을 정확히 집계한다")
    void getSummaryAggregatesMetricsAccurately() {
        LocalDate targetDate = LocalDate.of(2026, 11, 27);
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime nextDayStart = targetDate.plusDays(1).atStartOfDay();

        Item box = Item.builder()
                .itemCode("BOX-001")
                .itemName("택배박스 3호")
                .itemType(ItemType.FINISHED_GOOD)
                .unit("개")
                .safetyStock(50)
                .active(true)
                .build();

        WorkOrder inProgressOrder = WorkOrder.builder()
                .orderNumber("WO-20261127-001")
                .item(box)
                .targetQuantity(200)
                .plannedDate(targetDate)
                .build();
        inProgressOrder.recordProduction(93, 7);

        ProductionResult firstResult = ProductionResult.builder()
                .workOrder(inProgressOrder)
                .goodQuantity(93)
                .defectQuantity(7)
                .workerName("김작업")
                .build();

        ProductionResult secondResult = ProductionResult.builder()
                .workOrder(inProgressOrder)
                .goodQuantity(45)
                .defectQuantity(5)
                .workerName("이작업")
                .build();

        InventoryResponse lowStockItem = InventoryResponse.builder()
                .itemId(1L)
                .itemCode("RAW-001")
                .itemName("AB골 원단")
                .itemType(ItemType.RAW_MATERIAL)
                .itemTypeDescription("원재료")
                .unit("매")
                .quantity(20)
                .safetyStock(100)
                .belowSafetyStock(true)
                .active(true)
                .build();

        WorkOrder pausedOrder = WorkOrder.builder()
                .orderNumber("WO-20261127-002")
                .item(box)
                .targetQuantity(150)
                .plannedDate(targetDate)
                .build();
        pausedOrder.changeStatus(WorkOrderStatus.IN_PROGRESS);
        pausedOrder.changeStatus(WorkOrderStatus.PAUSED, "원자재 부족");

        given(workOrderRepository.countByStatus(WorkOrderStatus.WAITING)).willReturn(3L);
        given(workOrderRepository.findByStatusOrderByIdDesc(WorkOrderStatus.IN_PROGRESS))
                .willReturn(List.of(inProgressOrder));
        given(workOrderRepository.findByStatusOrderByIdDesc(WorkOrderStatus.PAUSED))
                .willReturn(List.of(pausedOrder));
        given(workOrderRepository.countByStatus(WorkOrderStatus.COMPLETED)).willReturn(5L);
        given(productionResultRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdDesc(
                startOfDay, nextDayStart))
                .willReturn(List.of(firstResult, secondResult));
        given(inventoryService.getInventories(null, true)).willReturn(List.of(lowStockItem));

        DashboardSummaryResponse summary = dashboardService.getSummary(targetDate);

        assertThat(summary.getBaseDate()).isEqualTo(targetDate);
        assertThat(summary.getWaitingWorkOrderCount()).isEqualTo(3L);
        assertThat(summary.getInProgressWorkOrderCount()).isEqualTo(1L);
        assertThat(summary.getPausedWorkOrderCount()).isEqualTo(1L);
        assertThat(summary.getCompletedWorkOrderCount()).isEqualTo(5L);

        // 양품 93 + 45 = 138, 불량 7 + 5 = 12, 총 150, 불량률 = 12 / 150 * 100 = 8.0%
        assertThat(summary.getTodayResultCount()).isEqualTo(2);
        assertThat(summary.getTodayGoodQuantity()).isEqualTo(138);
        assertThat(summary.getTodayDefectQuantity()).isEqualTo(12);
        assertThat(summary.getTodayTotalQuantity()).isEqualTo(150);
        assertThat(summary.getTodayDefectRate()).isEqualTo(8.0);

        assertThat(summary.getBelowSafetyStockCount()).isEqualTo(1);
        assertThat(summary.getBelowSafetyStockItems()).hasSize(1);
        assertThat(summary.getBelowSafetyStockItems().get(0).getItemCode()).isEqualTo("RAW-001");
        assertThat(summary.getInProgressWorkOrders()).hasSize(1);
        assertThat(summary.getInProgressWorkOrders().get(0).getOrderNumber()).isEqualTo("WO-20261127-001");
        assertThat(summary.getPausedWorkOrders()).hasSize(1);
        assertThat(summary.getPausedWorkOrders().get(0).getPauseReason()).isEqualTo("원자재 부족");
    }

    @Test
    @DisplayName("생산실적이 없으면 총 생산량은 0, 불량률은 0.0으로 안전하게 반환되며 날짜 미지정 시 오늘 날짜를 사용한다")
    void getSummaryWithNoProductionReturnsZeroDefectRateAndDefaultsToToday() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime nextDayStart = today.plusDays(1).atStartOfDay();

        given(workOrderRepository.countByStatus(WorkOrderStatus.WAITING)).willReturn(0L);
        given(workOrderRepository.findByStatusOrderByIdDesc(WorkOrderStatus.IN_PROGRESS)).willReturn(List.of());
        given(workOrderRepository.countByStatus(WorkOrderStatus.COMPLETED)).willReturn(0L);
        given(productionResultRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdDesc(
                startOfDay, nextDayStart))
                .willReturn(List.of());
        given(inventoryService.getInventories(null, true)).willReturn(List.of());

        DashboardSummaryResponse summary = dashboardService.getSummary(null);

        assertThat(summary.getBaseDate()).isEqualTo(today);
        assertThat(summary.getTodayResultCount()).isZero();
        assertThat(summary.getTodayGoodQuantity()).isZero();
        assertThat(summary.getTodayDefectQuantity()).isZero();
        assertThat(summary.getTodayTotalQuantity()).isZero();
        assertThat(summary.getTodayDefectRate()).isEqualTo(0.0);
        assertThat(summary.getBelowSafetyStockCount()).isZero();
        assertThat(summary.getInProgressWorkOrders()).isEmpty();

        verify(productionResultRepository)
                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdDesc(startOfDay, nextDayStart);
    }

    @Test
    @DisplayName("불량률은 소수점 첫째 자리에서 반올림한다")
    void getSummaryRoundsDefectRateToOneDecimalPlace() {
        LocalDate targetDate = LocalDate.of(2026, 11, 27);
        ProductionResult result = ProductionResult.builder()
                .goodQuantity(2)
                .defectQuantity(1)
                .build();

        given(workOrderRepository.countByStatus(WorkOrderStatus.WAITING)).willReturn(0L);
        given(workOrderRepository.findByStatusOrderByIdDesc(WorkOrderStatus.IN_PROGRESS)).willReturn(List.of());
        given(workOrderRepository.countByStatus(WorkOrderStatus.COMPLETED)).willReturn(0L);
        given(productionResultRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdDesc(
                targetDate.atStartOfDay(), targetDate.plusDays(1).atStartOfDay()))
                .willReturn(List.of(result));
        given(inventoryService.getInventories(null, true)).willReturn(List.of());

        DashboardSummaryResponse summary = dashboardService.getSummary(targetDate);

        // 1 / 3 * 100 = 33.333... -> 33.3
        assertThat(summary.getTodayDefectRate()).isEqualTo(33.3);
    }
}
