package com.lgb.factoryflow.domain.production.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductionResultCreateRequest {

    @NotNull(message = "양품 수량은 필수입니다.")
    @Min(value = 0, message = "양품 수량은 0 이상이어야 합니다.")
    private Integer goodQuantity;

    @NotNull(message = "불량 수량은 필수입니다.")
    @Min(value = 0, message = "불량 수량은 0 이상이어야 합니다.")
    private Integer defectQuantity;

    @Size(max = 50, message = "작업자명은 50자 이하로 입력해 주세요.")
    private String workerName;

    @Size(max = 255, message = "비고는 255자 이하로 입력해 주세요.")
    private String remark;

    public ProductionResultCreateRequest(
            Integer goodQuantity,
            Integer defectQuantity,
            String workerName,
            String remark
    ) {
        this.goodQuantity = goodQuantity;
        this.defectQuantity = defectQuantity;
        this.workerName = workerName;
        this.remark = remark;
    }
}
