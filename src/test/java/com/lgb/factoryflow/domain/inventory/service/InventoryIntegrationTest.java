package com.lgb.factoryflow.domain.inventory.service;

import com.lgb.factoryflow.domain.inventory.dto.InventoryHistoryResponse;
import com.lgb.factoryflow.domain.inventory.dto.InventoryInboundRequest;
import com.lgb.factoryflow.domain.inventory.dto.InventoryOutboundRequest;
import com.lgb.factoryflow.domain.inventory.dto.InventoryResponse;
import com.lgb.factoryflow.domain.inventory.entity.InventoryTransactionType;
import com.lgb.factoryflow.domain.item.entity.Item;
import com.lgb.factoryflow.domain.item.entity.ItemType;
import com.lgb.factoryflow.domain.item.repository.ItemRepository;
import com.lgb.factoryflow.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class InventoryIntegrationTest {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private InventoryService inventoryService;

    @Test
    @DisplayName("실제 MySQL DB에서 입고·출고·재고부족 차단 및 이력 조회가 정합성 있게 동작한다")
    void inboundOutboundAndInsufficientStockWithDatabase() {
        String code = "IT-RAW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Item item = itemRepository.save(Item.builder()
                .itemCode(code)
                .itemName("통합테스트 AB골 원단")
                .itemType(ItemType.RAW_MATERIAL)
                .spec("1200x1000")
                .unit("매")
                .safetyStock(50)
                .active(true)
                .build());

        InventoryHistoryResponse inbound = inventoryService.inbound(
                new InventoryInboundRequest(item.getId(), null, 80, "초기 입고")
        );
        assertThat(inbound.getAfterQuantity()).isEqualTo(80);

        InventoryHistoryResponse outbound = inventoryService.outbound(
                new InventoryOutboundRequest(item.getId(), null, 50, "공정 출고")
        );
        assertThat(outbound.getAfterQuantity()).isEqualTo(30);

        InventoryResponse current = inventoryService.getInventoryByItemId(item.getId());
        assertThat(current.getQuantity()).isEqualTo(30);
        assertThat(current.isBelowSafetyStock()).isTrue();

        assertThatThrownBy(() -> inventoryService.outbound(
                new InventoryOutboundRequest(item.getId(), null, 40, "초과 출고 시도")
        )).isInstanceOf(BusinessException.class);

        List<InventoryHistoryResponse> histories = inventoryService.getHistories(item.getId(), null);
        assertThat(histories).hasSize(2);
        assertThat(histories.get(0).getTransactionType()).isEqualTo(InventoryTransactionType.OUTBOUND);
        assertThat(histories.get(1).getTransactionType()).isEqualTo(InventoryTransactionType.INBOUND);
    }

    @Test
    @DisplayName("비활성화된 품목은 안전재고 미만 경고 목록(belowSafetyOnly=true)에서 제외된다")
    void inactiveItemExcludedFromBelowSafetyStockAlerts() {
        String code = "IT-OFF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Item item = itemRepository.save(Item.builder()
                .itemCode(code)
                .itemName("단종 테스트 원단")
                .itemType(ItemType.RAW_MATERIAL)
                .unit("매")
                .safetyStock(100)
                .active(true)
                .build());

        inventoryService.inbound(new InventoryInboundRequest(item.getId(), null, 10, "소량 입고"));
        item.changeActive(false);
        itemRepository.flush();

        List<InventoryResponse> lowStocks = inventoryService.getInventories(ItemType.RAW_MATERIAL, true);
        assertThat(lowStocks).noneMatch(inv -> inv.getItemId().equals(item.getId()));
    }
}
