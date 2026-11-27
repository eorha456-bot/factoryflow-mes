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
import com.lgb.factoryflow.domain.partner.repository.PartnerRepository;
import com.lgb.factoryflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final ItemRepository itemRepository;
    private final PartnerRepository partnerRepository;

    public List<InventoryResponse> getInventories(ItemType itemType, Boolean belowSafetyOnly) {
        List<Inventory> existingInventories = itemType != null
                ? inventoryRepository.findByItemItemTypeOrderByItemIdAsc(itemType)
                : inventoryRepository.findAllByOrderByItemIdAsc();

        Map<Long, InventoryResponse> responseByItemId = new LinkedHashMap<>();
        for (Inventory inventory : existingInventories) {
            responseByItemId.put(inventory.getItem().getId(), InventoryResponse.from(inventory));
        }

        List<Item> activeItems = itemType != null
                ? itemRepository.findByItemTypeAndActiveOrderByIdDesc(itemType, true)
                : itemRepository.findByActiveOrderByIdDesc(true);

        for (Item item : activeItems) {
            responseByItemId.putIfAbsent(item.getId(), InventoryResponse.emptyOf(item));
        }

        List<InventoryResponse> results = new ArrayList<>(responseByItemId.values());
        if (Boolean.TRUE.equals(belowSafetyOnly)) {
            return results.stream()
                    .filter(response -> response.isActive() && response.isBelowSafetyStock())
                    .toList();
        }
        return results;
    }

    public InventoryResponse getInventoryByItemId(Long itemId) {
        Item item = findItemById(itemId);
        return inventoryRepository.findByItemId(itemId)
                .map(InventoryResponse::from)
                .orElseGet(() -> InventoryResponse.emptyOf(item));
    }

    public List<InventoryHistoryResponse> getHistories(Long itemId, InventoryTransactionType transactionType) {
        List<InventoryHistory> histories;
        if (itemId != null && transactionType != null) {
            histories = inventoryHistoryRepository.findByItemIdAndTransactionTypeOrderByIdDesc(itemId, transactionType);
        } else if (itemId != null) {
            histories = inventoryHistoryRepository.findByItemIdOrderByIdDesc(itemId);
        } else if (transactionType != null) {
            histories = inventoryHistoryRepository.findByTransactionTypeOrderByIdDesc(transactionType);
        } else {
            histories = inventoryHistoryRepository.findAllByOrderByIdDesc();
        }
        return histories.stream()
                .map(InventoryHistoryResponse::from)
                .toList();
    }

    @Transactional
    public InventoryHistoryResponse inbound(InventoryInboundRequest request) {
        Item item = findActiveItemById(request.getItemId());
        Partner partner = findActivePartnerIfPresent(request.getPartnerId());
        return increaseStock(item, partner, InventoryTransactionType.INBOUND, request.getQuantity(), request.getRemark());
    }

    @Transactional
    public InventoryHistoryResponse outbound(InventoryOutboundRequest request) {
        Item item = findActiveItemById(request.getItemId());
        Partner partner = findActivePartnerIfPresent(request.getPartnerId());
        return decreaseStock(item, partner, InventoryTransactionType.OUTBOUND, request.getQuantity(), request.getRemark());
    }

    @Transactional
    public InventoryHistoryResponse adjust(InventoryAdjustRequest request) {
        Item item = findActiveItemById(request.getItemId());
        Inventory inventory = getOrCreateInventory(item);

        int beforeQuantity = inventory.getQuantity();
        int delta = inventory.adjustTo(request.getTargetQuantity());
        int afterQuantity = inventory.getQuantity();

        InventoryHistory history = InventoryHistory.builder()
                .item(item)
                .partner(null)
                .transactionType(InventoryTransactionType.ADJUST)
                .beforeQuantity(beforeQuantity)
                .changeQuantity(delta)
                .afterQuantity(afterQuantity)
                .remark(trimToNull(request.getRemark()))
                .build();

        return InventoryHistoryResponse.from(inventoryHistoryRepository.save(history));
    }

    @Transactional
    public InventoryHistoryResponse increaseForProduction(Item item, int quantity, String remark) {
        validateActiveItem(item);
        return increaseStock(item, null, InventoryTransactionType.PRODUCTION_IN, quantity, remark);
    }

    @Transactional
    public InventoryHistoryResponse decreaseForProduction(Item item, int quantity, String remark) {
        validateActiveItem(item);
        return decreaseStock(item, null, InventoryTransactionType.PRODUCTION_OUT, quantity, remark);
    }

    private InventoryHistoryResponse increaseStock(
            Item item,
            Partner partner,
            InventoryTransactionType transactionType,
            int quantity,
            String remark
    ) {
        Inventory inventory = getOrCreateInventory(item);
        int beforeQuantity = inventory.getQuantity();
        inventory.increase(quantity);
        int afterQuantity = inventory.getQuantity();

        InventoryHistory history = InventoryHistory.builder()
                .item(item)
                .partner(partner)
                .transactionType(transactionType)
                .beforeQuantity(beforeQuantity)
                .changeQuantity(quantity)
                .afterQuantity(afterQuantity)
                .remark(trimToNull(remark))
                .build();

        return InventoryHistoryResponse.from(inventoryHistoryRepository.save(history));
    }

    private InventoryHistoryResponse decreaseStock(
            Item item,
            Partner partner,
            InventoryTransactionType transactionType,
            int quantity,
            String remark
    ) {
        Inventory inventory = inventoryRepository.findByItemId(item.getId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        String.format("재고가 부족합니다. 품목=%s, 현재고=0, 요청수량=%d", item.getItemName(), quantity)
                ));

        int beforeQuantity = inventory.getQuantity();
        inventory.decrease(quantity);
        int afterQuantity = inventory.getQuantity();

        InventoryHistory history = InventoryHistory.builder()
                .item(item)
                .partner(partner)
                .transactionType(transactionType)
                .beforeQuantity(beforeQuantity)
                .changeQuantity(-quantity)
                .afterQuantity(afterQuantity)
                .remark(trimToNull(remark))
                .build();

        return InventoryHistoryResponse.from(inventoryHistoryRepository.save(history));
    }

    private Inventory getOrCreateInventory(Item item) {
        return inventoryRepository.findByItemId(item.getId())
                .orElseGet(() -> inventoryRepository.save(Inventory.createEmpty(item)));
    }

    private Item findItemById(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "품목을 찾을 수 없습니다. id=" + itemId));
    }

    private Item findActiveItemById(Long itemId) {
        Item item = findItemById(itemId);
        validateActiveItem(item);
        return item;
    }

    private void validateActiveItem(Item item) {
        if (!item.isActive()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "비활성화된 품목은 재고 입출고할 수 없습니다.");
        }
    }

    private Partner findActivePartnerIfPresent(Long partnerId) {
        if (partnerId == null) {
            return null;
        }
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "거래처를 찾을 수 없습니다. id=" + partnerId));
        if (!partner.isActive()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "비활성화된 거래처는 선택할 수 없습니다.");
        }
        return partner;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
