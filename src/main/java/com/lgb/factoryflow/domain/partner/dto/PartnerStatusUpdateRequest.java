package com.lgb.factoryflow.domain.partner.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PartnerStatusUpdateRequest {

    @NotNull(message = "활성 상태(active) 값은 필수입니다.")
    private Boolean active;

    public PartnerStatusUpdateRequest(Boolean active) {
        this.active = active;
    }
}
