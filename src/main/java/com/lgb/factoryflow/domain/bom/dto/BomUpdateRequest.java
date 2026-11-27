package com.lgb.factoryflow.domain.bom.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BomUpdateRequest {

    @NotNull(message = "단위당 소요 수량은 필수입니다.")
    @Positive(message = "소요 수량은 1 이상이어야 합니다.")
    private Integer requiredQuantity;

    public BomUpdateRequest(Integer requiredQuantity) {
        this.requiredQuantity = requiredQuantity;
    }
}
