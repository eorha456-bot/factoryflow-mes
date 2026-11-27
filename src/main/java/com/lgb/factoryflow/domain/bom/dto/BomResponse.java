package com.lgb.factoryflow.domain.bom.dto;

import com.lgb.factoryflow.domain.bom.entity.Bom;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BomResponse {

    private final Long id;
    private final Long finishedItemId;
    private final String finishedItemCode;
    private final String finishedItemName;
    private final Long rawMaterialId;
    private final String rawMaterialCode;
    private final String rawMaterialName;
    private final String rawMaterialUnit;
    private final int requiredQuantity;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static BomResponse from(Bom bom) {
        return BomResponse.builder()
                .id(bom.getId())
                .finishedItemId(bom.getFinishedItem().getId())
                .finishedItemCode(bom.getFinishedItem().getItemCode())
                .finishedItemName(bom.getFinishedItem().getItemName())
                .rawMaterialId(bom.getRawMaterial().getId())
                .rawMaterialCode(bom.getRawMaterial().getItemCode())
                .rawMaterialName(bom.getRawMaterial().getItemName())
                .rawMaterialUnit(bom.getRawMaterial().getUnit())
                .requiredQuantity(bom.getRequiredQuantity())
                .createdAt(bom.getCreatedAt())
                .updatedAt(bom.getUpdatedAt())
                .build();
    }
}
