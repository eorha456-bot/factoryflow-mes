package com.lgb.factoryflow.domain.production.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class WorkOrderCreateRequest {

    @Size(max = 50, message = "작업지시 번호는 50자 이하로 입력해 주세요.")
    private String orderNumber;

    @NotNull(message = "생산할 완제품 품목 ID는 필수입니다.")
    private Long itemId;

    @NotNull(message = "목표 생산 수량은 필수입니다.")
    @Positive(message = "목표 생산 수량은 1 이상이어야 합니다.")
    private Integer targetQuantity;

    @NotNull(message = "작업 예정일은 필수입니다.")
    private LocalDate plannedDate;

    @Size(max = 255, message = "비고는 255자 이하로 입력해 주세요.")
    private String remark;

    public WorkOrderCreateRequest(
            String orderNumber,
            Long itemId,
            Integer targetQuantity,
            LocalDate plannedDate,
            String remark
    ) {
        this.orderNumber = orderNumber;
        this.itemId = itemId;
        this.targetQuantity = targetQuantity;
        this.plannedDate = plannedDate;
        this.remark = remark;
    }
}
