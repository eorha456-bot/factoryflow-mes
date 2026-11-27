package com.lgb.factoryflow.domain.bom.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BomCreateRequest {

    @NotNull(message = "완제품 품목 ID는 필수입니다.")
    private Long finishedItemId;

    @NotNull(message = "원재료 품목 ID는 필수입니다.")
    private Long rawMaterialId;

    @NotNull(message = "단위당 소요 수량은 필수입니다.")
    @Positive(message = "소요 수량은 1 이상이어야 합니다.")
    private Integer requiredQuantity;

    public BomCreateRequest(Long finishedItemId, Long rawMaterialId, Integer requiredQuantity) {
        this.finishedItemId = finishedItemId;
        this.rawMaterialId = rawMaterialId;
        this.requiredQuantity = requiredQuantity;
    }
}
