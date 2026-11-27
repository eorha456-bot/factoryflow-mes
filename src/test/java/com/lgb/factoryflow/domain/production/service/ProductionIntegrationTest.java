package com.lgb.factoryflow.domain.production.service;

import com.lgb.factoryflow.domain.bom.dto.BomCreateRequest;
import com.lgb.factoryflow.domain.bom.repository.BomRepository;
import com.lgb.factoryflow.domain.bom.service.BomService;
import com.lgb.factoryflow.domain.inventory.dto.InventoryHistoryResponse;
import com.lgb.factoryflow.domain.inventory.dto.InventoryInboundRequest;
import com.lgb.factoryflow.domain.inventory.entity.InventoryTransactionType;
import com.lgb.factoryflow.domain.inventory.repository.InventoryHistoryRepository;
import com.lgb.factoryflow.domain.inventory.repository.InventoryRepository;
import com.lgb.factoryflow.domain.inventory.service.InventoryService;
import com.lgb.factoryflow.domain.item.entity.Item;
import com.lgb.factoryflow.domain.item.entity.ItemType;
import com.lgb.factoryflow.domain.item.repository.ItemRepository;
import com.lgb.factoryflow.domain.production.dto.ProductionResultCreateRequest;
import com.lgb.factoryflow.domain.production.dto.ProductionResultResponse;
import com.lgb.factoryflow.domain.production.dto.WorkOrderCreateRequest;
import com.lgb.factoryflow.domain.production.dto.WorkOrderResponse;
import com.lgb.factoryflow.domain.production.entity.WorkOrderStatus;
import com.lgb.factoryflow.domain.production.repository.ProductionResultRepository;
import com.lgb.factoryflow.domain.production.repository.WorkOrderRepository;
import com.lgb.factoryflow.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ProductionIntegrationTest {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private BomService bomService;

    @Autowired
    private BomRepository bomRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryHistoryRepository inventoryHistoryRepository;

    @Autowired
    private ProductionService productionService;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private ProductionResultRepository productionResultRepository;

    @Test
    @Transactional
    @DisplayName("생산실적 등록 시 BOM 기준으로 원재료(양품+불량)가 자동 차감되고 완제품(양품)이 자동 증가하며 목표 달성 시 완료된다")
    void recordProductionResultDeductsRawMaterialAndIncreasesFinishedGood() {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Item sheet = itemRepository.save(Item.builder()
                .itemCode("IT-SHEET-" + suffix)
                .itemName("AB골 원단")
                .itemType(ItemType.RAW_MATERIAL)
                .unit("매")
                .safetyStock(50)
                .active(true)
                .build());

        Item box = itemRepository.save(Item.builder()
                .itemCode("IT-BOX-" + suffix)
                .itemName("택배박스 3호")
                .itemType(ItemType.FINISHED_GOOD)
                .unit("개")
                .safetyStock(30)
                .active(true)
                .build());

        // 박스 1개당 원단 2매 소요
        bomService.createBom(new BomCreateRequest(box.getId(), sheet.getId(), 2));

        // 원단 300매 입고
        inventoryService.inbound(new InventoryInboundRequest(sheet.getId(), null, 300, "테스트 입고"));

        // 박스 100개 작업지시 생성
        WorkOrderResponse order = productionService.createWorkOrder(new WorkOrderCreateRequest(
                "WO-IT-" + suffix,
                box.getId(),
                100,
                LocalDate.of(2026, 11, 27),
                "1차 생산"
        ));

        // 1회차 실적: 양품 60개 + 불량 5개 (총 65개 생산 -> 원단 130매 차감, 완제품 60개 증가, 상태 IN_PROGRESS)
        ProductionResultResponse first = productionService.recordProductionResult(
                order.getId(),
                new ProductionResultCreateRequest(60, 5, "김작업", "오전 실적")
        );

        assertThat(first.getWorkOrderStatus()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
        assertThat(inventoryService.getInventoryByItemId(sheet.getId()).getQuantity()).isEqualTo(170);
        assertThat(inventoryService.getInventoryByItemId(box.getId()).getQuantity()).isEqualTo(60);

        // 2회차 실적: 양품 40개 + 불량 0개 (총 40개 생산 -> 원단 80매 차감, 완제품 40개 증가 -> 누적 양품 100개 -> COMPLETED)
        ProductionResultResponse second = productionService.recordProductionResult(
                order.getId(),
                new ProductionResultCreateRequest(40, 0, "김작업", "오후 마감")
        );

        assertThat(second.getWorkOrderStatus()).isEqualTo(WorkOrderStatus.COMPLETED);
        assertThat(second.getCumulativeGoodQuantity()).isEqualTo(100);
        assertThat(second.getCumulativeDefectQuantity()).isEqualTo(5);
        assertThat(inventoryService.getInventoryByItemId(sheet.getId()).getQuantity()).isEqualTo(90);
        assertThat(inventoryService.getInventoryByItemId(box.getId()).getQuantity()).isEqualTo(100);

        List<InventoryHistoryResponse> sheetHistories = inventoryService.getHistories(
                sheet.getId(), InventoryTransactionType.PRODUCTION_OUT
        );
        assertThat(sheetHistories).hasSize(2);
        assertThat(sheetHistories.get(0).getChangeQuantity()).isEqualTo(-80);
        assertThat(sheetHistories.get(1).getChangeQuantity()).isEqualTo(-130);

        List<InventoryHistoryResponse> boxHistories = inventoryService.getHistories(
                box.getId(), InventoryTransactionType.PRODUCTION_IN
        );
        assertThat(boxHistories).hasSize(2);
        assertThat(boxHistories.get(0).getChangeQuantity()).isEqualTo(40);
        assertThat(boxHistories.get(1).getChangeQuantity()).isEqualTo(60);
    }

    @Test
    @DisplayName("BOM 원재료 중 하나라도 재고가 부족하면 생산실적 등록 전체 트랜잭션이 롤백된다")
    void recordProductionResultRollsBackAllChangesWhenRawMaterialInsufficient() {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Item sheet = itemRepository.save(Item.builder()
                .itemCode("RB-SHEET-" + suffix)
                .itemName("롤백테스트 원단")
                .itemType(ItemType.RAW_MATERIAL)
                .unit("매")
                .safetyStock(10)
                .active(true)
                .build());

        Item glue = itemRepository.save(Item.builder()
                .itemCode("RB-GLUE-" + suffix)
                .itemName("롤백테스트 접착제")
                .itemType(ItemType.RAW_MATERIAL)
                .unit("개")
                .safetyStock(10)
                .active(true)
                .build());

        Item box = itemRepository.save(Item.builder()
                .itemCode("RB-BOX-" + suffix)
                .itemName("롤백테스트 박스")
                .itemType(ItemType.FINISHED_GOOD)
                .unit("개")
                .safetyStock(10)
                .active(true)
                .build());

        Long bom1Id = bomService.createBom(new BomCreateRequest(box.getId(), sheet.getId(), 2)).getId();
        Long bom2Id = bomService.createBom(new BomCreateRequest(box.getId(), glue.getId(), 1)).getId();

        // 원단은 충분(100매), 접착제는 부족(5개)
        inventoryService.inbound(new InventoryInboundRequest(sheet.getId(), null, 100, "원단 입고"));
        inventoryService.inbound(new InventoryInboundRequest(glue.getId(), null, 5, "접착제 입고"));

        WorkOrderResponse order = productionService.createWorkOrder(new WorkOrderCreateRequest(
                "WO-RB-" + suffix,
                box.getId(),
                50,
                LocalDate.of(2026, 11, 27),
                "롤백 검증"
        ));

        try {
            // 10개 생산 시도 -> 원단 20매 차감 후 접착제 10개 차감 단계에서 재고 부족 발생해야 함
            assertThatThrownBy(() -> productionService.recordProductionResult(
                    order.getId(),
                    new ProductionResultCreateRequest(10, 0, "박작업", "실패 테스트")
            ))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("재고가 부족합니다");

            // 트랜잭션 롤백 검증: 먼저 차감되었던 원단도 100매 그대로 유지되어야 함
            assertThat(inventoryService.getInventoryByItemId(sheet.getId()).getQuantity()).isEqualTo(100);
            assertThat(inventoryService.getInventoryByItemId(glue.getId()).getQuantity()).isEqualTo(5);
            assertThat(inventoryService.getInventoryByItemId(box.getId()).getQuantity()).isEqualTo(0);

            WorkOrderResponse unchangedOrder = productionService.getWorkOrder(order.getId());
            assertThat(unchangedOrder.getStatus()).isEqualTo(WorkOrderStatus.WAITING);
            assertThat(unchangedOrder.getProducedGoodQuantity()).isEqualTo(0);
            assertThat(productionService.getProductionResultsByWorkOrder(order.getId())).isEmpty();
        } finally {
            productionResultRepository.deleteAll(
                    productionResultRepository.findByWorkOrderIdOrderByIdDesc(order.getId())
            );
            workOrderRepository.deleteById(order.getId());
            bomRepository.deleteById(bom2Id);
            bomRepository.deleteById(bom1Id);
            inventoryHistoryRepository.deleteAll(inventoryHistoryRepository.findByItemIdOrderByIdDesc(box.getId()));
            inventoryHistoryRepository.deleteAll(inventoryHistoryRepository.findByItemIdOrderByIdDesc(glue.getId()));
            inventoryHistoryRepository.deleteAll(inventoryHistoryRepository.findByItemIdOrderByIdDesc(sheet.getId()));
            inventoryRepository.findByItemId(box.getId()).ifPresent(inventoryRepository::delete);
            inventoryRepository.findByItemId(glue.getId()).ifPresent(inventoryRepository::delete);
            inventoryRepository.findByItemId(sheet.getId()).ifPresent(inventoryRepository::delete);
            itemRepository.deleteById(box.getId());
            itemRepository.deleteById(glue.getId());
            itemRepository.deleteById(sheet.getId());
        }
    }
}
