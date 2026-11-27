package com.lgb.factoryflow.domain.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InventoryInboundRequest {

    @NotNull(message = "품목 ID는 필수입니다.")
    private Long itemId;

    private Long partnerId;

    @NotNull(message = "입고 수량은 필수입니다.")
    @Positive(message = "입고 수량은 1 이상이어야 합니다.")
    private Integer quantity;

    @Size(max = 255, message = "비고는 255자 이하로 입력해 주세요.")
    private String remark;

    public InventoryInboundRequest(Long itemId, Long partnerId, Integer quantity, String remark) {
        this.itemId = itemId;
        this.partnerId = partnerId;
        this.quantity = quantity;
        this.remark = remark;
    }
}
