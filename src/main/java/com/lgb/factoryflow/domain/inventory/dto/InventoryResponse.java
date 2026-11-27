package com.lgb.factoryflow.domain.inventory.dto;

import com.lgb.factoryflow.domain.inventory.entity.Inventory;
import com.lgb.factoryflow.domain.item.entity.Item;
import com.lgb.factoryflow.domain.item.entity.ItemType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InventoryResponse {

    private final Long inventoryId;
    private final Long itemId;
    private final String itemCode;
    private final String itemName;
    private final ItemType itemType;
    private final String itemTypeDescription;
    private final String spec;
    private final String unit;
    private final int quantity;
    private final int safetyStock;
    private final boolean belowSafetyStock;
    private final boolean active;
    private final LocalDateTime updatedAt;

    public static InventoryResponse from(Inventory inventory) {
        Item item = inventory.getItem();
        return InventoryResponse.builder()
                .inventoryId(inventory.getId())
                .itemId(item.getId())
                .itemCode(item.getItemCode())
                .itemName(item.getItemName())
                .itemType(item.getItemType())
                .itemTypeDescription(item.getItemType().getDescription())
                .spec(item.getSpec())
                .unit(item.getUnit())
                .quantity(inventory.getQuantity())
                .safetyStock(item.getSafetyStock())
                .belowSafetyStock(inventory.isBelowSafetyStock())
                .active(item.isActive())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }

    public static InventoryResponse emptyOf(Item item) {
        return InventoryResponse.builder()
                .inventoryId(null)
                .itemId(item.getId())
                .itemCode(item.getItemCode())
                .itemName(item.getItemName())
                .itemType(item.getItemType())
                .itemTypeDescription(item.getItemType().getDescription())
                .spec(item.getSpec())
                .unit(item.getUnit())
                .quantity(0)
                .safetyStock(item.getSafetyStock())
                .belowSafetyStock(0 < item.getSafetyStock())
                .active(item.isActive())
                .updatedAt(null)
                .build();
    }
}
