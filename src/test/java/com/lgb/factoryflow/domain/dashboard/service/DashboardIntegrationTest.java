package com.lgb.factoryflow.domain.dashboard.service;

import com.lgb.factoryflow.domain.dashboard.dto.DashboardSummaryResponse;
import com.lgb.factoryflow.domain.inventory.dto.InventoryInboundRequest;
import com.lgb.factoryflow.domain.inventory.service.InventoryService;
import com.lgb.factoryflow.domain.item.entity.Item;
import com.lgb.factoryflow.domain.item.entity.ItemType;
import com.lgb.factoryflow.domain.item.repository.ItemRepository;
import com.lgb.factoryflow.domain.production.dto.ProductionResultCreateRequest;
import com.lgb.factoryflow.domain.production.dto.WorkOrderCreateRequest;
import com.lgb.factoryflow.domain.production.dto.WorkOrderResponse;
import com.lgb.factoryflow.domain.production.dto.WorkOrderStatusUpdateRequest;
import com.lgb.factoryflow.domain.production.entity.WorkOrderStatus;
import com.lgb.factoryflow.domain.production.service.ProductionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DashboardIntegrationTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductionService productionService;

    @Test
    @Transactional
    @DisplayName("대시보드 요약 조회가 실제 DB의 작업지시 상태(일시정지 포함), 금일 생산실적, 활성 안전재고 미만 품목을 통합 집계한다")
    void getSummaryAggregatesRealDatabaseState() {
        LocalDate today = LocalDate.now();
        DashboardSummaryResponse before = dashboardService.getSummary(today);

        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 안전재고 미만 활성 원재료 (현재고 10 < 안전재고 50)
        Item lowStockSheet = itemRepository.save(Item.builder()
                .itemCode("DS-LOW-" + suffix)
                .itemName("대시보드 부족 원단")
                .itemType(ItemType.RAW_MATERIAL)
                .unit("매")
                .safetyStock(50)
                .active(true)
                .build());
        inventoryService.inbound(new InventoryInboundRequest(lowStockSheet.getId(), null, 10, "초기 입고"));

        // 안전재고 미만이지만 비활성 품목 -> 대시보드 경고에서 제외되어야 함
        itemRepository.save(Item.builder()
                .itemCode("DS-INACTIVE-" + suffix)
                .itemName("비활성 품목")
                .itemType(ItemType.RAW_MATERIAL)
                .unit("매")
                .safetyStock(100)
                .active(false)
                .build());

        // 완제품 (안전재고 10 -> 실적 90개 생산 후 안전재고 충족)
        Item box = itemRepository.save(Item.builder()
                .itemCode("DS-BOX-" + suffix)
                .itemName("대시보드 박스")
                .itemType(ItemType.FINISHED_GOOD)
                .unit("개")
                .safetyStock(10)
                .active(true)
                .build());

        // 대기 작업지시 1건
        productionService.createWorkOrder(new WorkOrderCreateRequest(
                "WO-DS-WAIT-" + suffix,
                box.getId(),
                50,
                today,
                "대기 건"
        ));

        // 진행 중 작업지시 1건 (목표 200, 양품 90, 불량 10 -> IN_PROGRESS 유지)
        WorkOrderResponse activeOrder = productionService.createWorkOrder(new WorkOrderCreateRequest(
                "WO-DS-PROG-" + suffix,
                box.getId(),
                200,
                today,
                "진행 중 건"
        ));
        productionService.recordProductionResult(
                activeOrder.getId(),
                new ProductionResultCreateRequest(90, 10, "김작업", "금일 실적")
        );

        // 일시정지 작업지시 1건
        WorkOrderResponse pausedOrder = productionService.createWorkOrder(new WorkOrderCreateRequest(
                "WO-DS-PAUSE-" + suffix,
                box.getId(),
                120,
                today,
                "정지 테스트"
        ));
        productionService.updateWorkOrderStatus(pausedOrder.getId(), new WorkOrderStatusUpdateRequest(WorkOrderStatus.IN_PROGRESS));
        productionService.updateWorkOrderStatus(pausedOrder.getId(), new WorkOrderStatusUpdateRequest(WorkOrderStatus.PAUSED, "설비 점검/고장"));

        DashboardSummaryResponse after = dashboardService.getSummary(today);

        assertThat(after.getBaseDate()).isEqualTo(today);
        assertThat(after.getWaitingWorkOrderCount()).isEqualTo(before.getWaitingWorkOrderCount() + 1);
        assertThat(after.getInProgressWorkOrderCount()).isEqualTo(before.getInProgressWorkOrderCount() + 1);
        assertThat(after.getPausedWorkOrderCount()).isEqualTo(before.getPausedWorkOrderCount() + 1);
        assertThat(after.getTodayResultCount()).isEqualTo(before.getTodayResultCount() + 1);
        assertThat(after.getTodayGoodQuantity()).isEqualTo(before.getTodayGoodQuantity() + 90);
        assertThat(after.getTodayDefectQuantity()).isEqualTo(before.getTodayDefectQuantity() + 10);
        assertThat(after.getTodayTotalQuantity()).isEqualTo(before.getTodayTotalQuantity() + 100);

        assertThat(after.getInProgressWorkOrders())
                .anySatisfy(wo -> {
                    assertThat(wo.getOrderNumber()).isEqualTo("WO-DS-PROG-" + suffix);
                    assertThat(wo.getProducedGoodQuantity()).isEqualTo(90);
                });

        assertThat(after.getPausedWorkOrders())
                .anySatisfy(wo -> {
                    assertThat(wo.getOrderNumber()).isEqualTo("WO-DS-PAUSE-" + suffix);
                    assertThat(wo.getPauseReason()).isEqualTo("설비 점검/고장");
                    assertThat(wo.getPausedAt()).isNotNull();
                });

        assertThat(after.getBelowSafetyStockItems())
                .anyMatch(inv -> inv.getItemCode().equals("DS-LOW-" + suffix))
                .noneMatch(inv -> inv.getItemCode().equals("DS-INACTIVE-" + suffix));
    }
}
