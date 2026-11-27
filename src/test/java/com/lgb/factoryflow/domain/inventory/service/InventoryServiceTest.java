package com.lgb.factoryflow.domain.inventory.service;

import com.lgb.factoryflow.domain.inventory.dto.InventoryAdjustRequest;
import com.lgb.factoryflow.domain.inventory.dto.InventoryHistoryResponse;
import com.lgb.factoryflow.domain.inventory.dto.InventoryInboundRequest;
import com.lgb.factoryflow.domain.inventory.dto.InventoryOutboundRequest;
import com.lgb.factoryflow.domain.inventory.dto.InventoryResponse;
import com.lgb.factoryflow.domain.inventory.entity.Inventory;
import com.lgb.factoryflow.domain.inventory.entity.InventoryHistory;
import com.lgb.factoryflow.domain.inventory.entity.InventoryTransactionType;
import com.lgb.factoryflow.domain.inventory.repository.InventoryHistoryRepository;
import com.lgb.factoryflow.domain.inventory.repository.InventoryRepository;
import com.lgb.factoryflow.domain.item.entity.Item;
import com.lgb.factoryflow.domain.item.entity.ItemType;
import com.lgb.factoryflow.domain.item.repository.ItemRepository;
import com.lgb.factoryflow.domain.partner.entity.Partner;
import com.lgb.factoryflow.domain.partner.entity.PartnerType;
import com.lgb.factoryflow.domain.partner.repository.PartnerRepository;
import com.lgb.factoryflow.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryHistoryRepository inventoryHistoryRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private PartnerRepository partnerRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    @DisplayName("입고 시 재고가 증가하고 INBOUND 이력이 기록된다")
    void inboundSuccess() {
        Item item = createActiveRawMaterial();
        Partner supplier = createActiveSupplier();
        Inventory inventory = Inventory.builder().item(item).quantity(20).build();

        given(itemRepository.findById(1L)).willReturn(Optional.of(item));
        given(partnerRepository.findById(10L)).willReturn(Optional.of(supplier));
        given(inventoryRepository.findByItemId(1L)).willReturn(Optional.of(inventory));
        given(inventoryHistoryRepository.save(any(InventoryHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        InventoryHistoryResponse response = inventoryService.inbound(
                new InventoryInboundRequest(1L, 10L, 50, "원단 정기 입고")
        );

        assertThat(inventory.getQuantity()).isEqualTo(70);
        assertThat(response.getTransactionType()).isEqualTo(InventoryTransactionType.INBOUND);
        assertThat(response.getBeforeQuantity()).isEqualTo(20);
        assertThat(response.getChangeQuantity()).isEqualTo(50);
        assertThat(response.getAfterQuantity()).isEqualTo(70);
        assertThat(response.getPartnerName()).isEqualTo("대한골판지");
    }

    @Test
    @DisplayName("출고 시 재고가 차감되고 음수 changeQuantity의 OUTBOUND 이력이 기록된다")
    void outboundSuccess() {
        Item item = createActiveRawMaterial();
        Inventory inventory = Inventory.builder().item(item).quantity(100).build();

        given(itemRepository.findById(1L)).willReturn(Optional.of(item));
        given(inventoryRepository.findByItemId(1L)).willReturn(Optional.of(inventory));
        given(inventoryHistoryRepository.save(any(InventoryHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        InventoryHistoryResponse response = inventoryService.outbound(
                new InventoryOutboundRequest(1L, null, 30, "긴급 출고")
        );

        assertThat(inventory.getQuantity()).isEqualTo(70);
        assertThat(response.getTransactionType()).isEqualTo(InventoryTransactionType.OUTBOUND);
        assertThat(response.getBeforeQuantity()).isEqualTo(100);
        assertThat(response.getChangeQuantity()).isEqualTo(-30);
        assertThat(response.getAfterQuantity()).isEqualTo(70);
    }

    @Test
    @DisplayName("현재고보다 많은 수량을 출고하면 400 BAD_REQUEST 예외가 발생하고 이력을 남기지 않는다")
    void outboundInsufficientStockThrowsBadRequest() {
        Item item = createActiveRawMaterial();
        Inventory inventory = Inventory.builder().item(item).quantity(10).build();

        given(itemRepository.findById(1L)).willReturn(Optional.of(item));
        given(inventoryRepository.findByItemId(1L)).willReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.outbound(
                new InventoryOutboundRequest(1L, null, 25, "초과 출고 시도")
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("재고가 부족합니다");

        assertThat(inventory.getQuantity()).isEqualTo(10);
        verify(inventoryHistoryRepository, never()).save(any(InventoryHistory.class));
    }

    @Test
    @DisplayName("비활성화된 품목은 입출고할 수 없다")
    void inboundInactiveItemThrowsBadRequest() {
        Item inactiveItem = Item.builder()
                .itemCode("RAW-OLD")
                .itemName("단종 원단")
                .itemType(ItemType.RAW_MATERIAL)
                .unit("매")
                .safetyStock(10)
                .active(false)
                .build();

        given(itemRepository.findById(2L)).willReturn(Optional.of(inactiveItem));

        assertThatThrownBy(() -> inventoryService.inbound(
                new InventoryInboundRequest(2L, null, 10, null)
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(inventoryHistoryRepository, never()).save(any(InventoryHistory.class));
    }

    @Test
    @DisplayName("재고 조정 시 목표 수량과 차이만큼 ADJUST 이력이 기록된다")
    void adjustStockSuccess() {
        Item item = createActiveRawMaterial();
        Inventory inventory = Inventory.builder().item(item).quantity(50).build();

        given(itemRepository.findById(1L)).willReturn(Optional.of(item));
        given(inventoryRepository.findByItemId(1L)).willReturn(Optional.of(inventory));
        given(inventoryHistoryRepository.save(any(InventoryHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        InventoryHistoryResponse response = inventoryService.adjust(
                new InventoryAdjustRequest(1L, 42, "실사 파손 8매 반영")
        );

        assertThat(inventory.getQuantity()).isEqualTo(42);
        assertThat(response.getTransactionType()).isEqualTo(InventoryTransactionType.ADJUST);
        assertThat(response.getBeforeQuantity()).isEqualTo(50);
        assertThat(response.getChangeQuantity()).isEqualTo(-8);
        assertThat(response.getAfterQuantity()).isEqualTo(42);
    }

    @Test
    @DisplayName("안전재고 미만 필터 조회 시 기준 미달 품목만 반환한다")
    void getInventoriesBelowSafetyOnly() {
        Item safeItem = Item.builder()
                .itemCode("RAW-01")
                .itemName("A골 원단")
                .itemType(ItemType.RAW_MATERIAL)
                .unit("매")
                .safetyStock(50)
                .active(true)
                .build();
        Item lowItem = Item.builder()
                .itemCode("BOX-01")
                .itemName("택배박스 1호")
                .itemType(ItemType.FINISHED_GOOD)
                .unit("개")
                .safetyStock(100)
                .active(true)
                .build();

        setItemId(safeItem, 1L);
        setItemId(lowItem, 2L);

        Inventory safeInventory = Inventory.builder().item(safeItem).quantity(80).build();
        Inventory lowInventory = Inventory.builder().item(lowItem).quantity(30).build();

        given(inventoryRepository.findAllByOrderByItemIdAsc()).willReturn(List.of(safeInventory, lowInventory));
        given(itemRepository.findByActiveOrderByIdDesc(true)).willReturn(List.of(safeItem, lowItem));

        List<InventoryResponse> lowStockList = inventoryService.getInventories(null, true);

        assertThat(lowStockList).hasSize(1);
        assertThat(lowStockList.get(0).getItemCode()).isEqualTo("BOX-01");
        assertThat(lowStockList.get(0).isBelowSafetyStock()).isTrue();
    }

    private Item createActiveRawMaterial() {
        Item item = Item.builder()
                .itemCode("RAW-AB-01")
                .itemName("AB골 원단")
                .itemType(ItemType.RAW_MATERIAL)
                .spec("1200x1000")
                .unit("매")
                .safetyStock(50)
                .active(true)
                .build();
        setItemId(item, 1L);
        return item;
    }

    private Partner createActiveSupplier() {
        return Partner.builder()
                .partnerCode("PTN-SUP-01")
                .partnerName("대한골판지")
                .partnerType(PartnerType.SUPPLIER)
                .active(true)
                .build();
    }

    private void setItemId(Item item, Long id) {
        try {
            var field = Item.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(item, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
