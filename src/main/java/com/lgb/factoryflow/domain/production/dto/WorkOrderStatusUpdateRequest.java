package com.lgb.factoryflow.domain.production.dto;

import com.lgb.factoryflow.domain.production.entity.WorkOrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WorkOrderStatusUpdateRequest {

    @NotNull(message = "변경할 작업지시 상태는 필수입니다.")
    private WorkOrderStatus status;

    public WorkOrderStatusUpdateRequest(WorkOrderStatus status) {
        this.status = status;
    }
}
