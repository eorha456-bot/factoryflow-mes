package com.lgb.factoryflow.domain.production.dto;

import com.lgb.factoryflow.domain.production.entity.WorkOrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WorkOrderStatusUpdateRequest {

    @NotNull(message = "변경할 작업지시 상태는 필수입니다.")
    private WorkOrderStatus status;

    @Size(max = 200, message = "사유는 200자 이하로 입력해 주세요.")
    private String reason;

    public WorkOrderStatusUpdateRequest(WorkOrderStatus status) {
        this(status, null);
    }

    public WorkOrderStatusUpdateRequest(WorkOrderStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }
}
