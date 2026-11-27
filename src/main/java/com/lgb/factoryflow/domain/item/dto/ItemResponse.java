package com.lgb.factoryflow.domain.item.dto;

import com.lgb.factoryflow.domain.item.entity.Item;
import com.lgb.factoryflow.domain.item.entity.ItemType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ItemResponse {

    private final Long id;
    private final String itemCode;
    private final String itemName;
    private final ItemType itemType;
    private final String itemTypeDescription;
    private final String spec;
    private final String unit;
    private final int safetyStock;
    private final boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static ItemResponse from(Item item) {
        return ItemResponse.builder()
                .id(item.getId())
                .itemCode(item.getItemCode())
                .itemName(item.getItemName())
                .itemType(item.getItemType())
                .itemTypeDescription(item.getItemType().getDescription())
                .spec(item.getSpec())
                .unit(item.getUnit())
                .safetyStock(item.getSafetyStock())
                .active(item.isActive())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
