package com.lgb.factoryflow.domain.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InventoryAdjustRequest {

    @NotNull(message = "품목 ID는 필수입니다.")
    private Long itemId;

    @NotNull(message = "조정 후 수량은 필수입니다.")
    @Min(value = 0, message = "조정 후 수량은 0 이상이어야 합니다.")
    private Integer targetQuantity;

    @NotBlank(message = "재고 조정 사유(비고)는 필수입니다.")
    @Size(max = 255, message = "비고는 255자 이하로 입력해 주세요.")
    private String remark;

    public InventoryAdjustRequest(Long itemId, Integer targetQuantity, String remark) {
        this.itemId = itemId;
        this.targetQuantity = targetQuantity;
        this.remark = remark;
    }
}
