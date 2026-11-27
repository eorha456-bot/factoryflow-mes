package com.lgb.factoryflow.domain.inventory.dto;

import com.lgb.factoryflow.domain.inventory.entity.InventoryHistory;
import com.lgb.factoryflow.domain.inventory.entity.InventoryTransactionType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InventoryHistoryResponse {

    private final Long id;
    private final Long itemId;
    private final String itemCode;
    private final String itemName;
    private final String unit;
    private final Long partnerId;
    private final String partnerName;
    private final InventoryTransactionType transactionType;
    private final String transactionTypeDescription;
    private final int beforeQuantity;
    private final int changeQuantity;
    private final int afterQuantity;
    private final String remark;
    private final LocalDateTime createdAt;

    public static InventoryHistoryResponse from(InventoryHistory history) {
        return InventoryHistoryResponse.builder()
                .id(history.getId())
                .itemId(history.getItem().getId())
                .itemCode(history.getItem().getItemCode())
                .itemName(history.getItem().getItemName())
                .unit(history.getItem().getUnit())
                .partnerId(history.getPartner() != null ? history.getPartner().getId() : null)
                .partnerName(history.getPartner() != null ? history.getPartner().getPartnerName() : null)
                .transactionType(history.getTransactionType())
                .transactionTypeDescription(history.getTransactionType().getDescription())
                .beforeQuantity(history.getBeforeQuantity())
                .changeQuantity(history.getChangeQuantity())
                .afterQuantity(history.getAfterQuantity())
                .remark(history.getRemark())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
