package com.lgb.factoryflow.domain.item.dto;

import com.lgb.factoryflow.domain.item.entity.Item;
import com.lgb.factoryflow.domain.item.entity.ItemType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ItemCreateRequest {

    @NotBlank(message = "품목 코드는 필수입니다.")
    @Size(max = 50, message = "품목 코드는 50자 이하로 입력해 주세요.")
    private String itemCode;

    @NotBlank(message = "품목명은 필수입니다.")
    @Size(max = 100, message = "품목명은 100자 이하로 입력해 주세요.")
    private String itemName;

    @NotNull(message = "품목 구분(RAW_MATERIAL 또는 FINISHED_GOOD)은 필수입니다.")
    private ItemType itemType;

    @Size(max = 100, message = "규격은 100자 이하로 입력해 주세요.")
    private String spec;

    @NotBlank(message = "단위는 필수입니다.")
    @Size(max = 20, message = "단위는 20자 이하로 입력해 주세요.")
    private String unit;

    @NotNull(message = "안전재고 수량은 필수입니다.")
    @Min(value = 0, message = "안전재고 수량은 0 이상이어야 합니다.")
    private Integer safetyStock;

    public ItemCreateRequest(
            String itemCode,
            String itemName,
            ItemType itemType,
            String spec,
            String unit,
            Integer safetyStock
    ) {
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.itemType = itemType;
        this.spec = spec;
        this.unit = unit;
        this.safetyStock = safetyStock;
    }

    public Item toEntity(String normalizedCode) {
        return Item.builder()
                .itemCode(normalizedCode)
                .itemName(itemName.trim())
                .itemType(itemType)
                .spec(spec == null || spec.isBlank() ? null : spec.trim())
                .unit(unit.trim())
                .safetyStock(safetyStock)
                .active(true)
                .build();
    }
}
