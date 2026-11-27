package com.lgb.factoryflow.domain.item.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ItemStatusUpdateRequest {

    @NotNull(message = "활성 상태(active) 값은 필수입니다.")
    private Boolean active;

    public ItemStatusUpdateRequest(Boolean active) {
        this.active = active;
    }
}
