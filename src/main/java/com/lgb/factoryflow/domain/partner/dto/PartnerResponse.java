package com.lgb.factoryflow.domain.partner.dto;

import com.lgb.factoryflow.domain.partner.entity.Partner;
import com.lgb.factoryflow.domain.partner.entity.PartnerType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PartnerResponse {

    private final Long id;
    private final String partnerCode;
    private final String partnerName;
    private final PartnerType partnerType;
    private final String partnerTypeDescription;
    private final String businessNumber;
    private final String phone;
    private final String address;
    private final boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static PartnerResponse from(Partner partner) {
        return PartnerResponse.builder()
                .id(partner.getId())
                .partnerCode(partner.getPartnerCode())
                .partnerName(partner.getPartnerName())
                .partnerType(partner.getPartnerType())
                .partnerTypeDescription(partner.getPartnerType().getDescription())
                .businessNumber(partner.getBusinessNumber())
                .phone(partner.getPhone())
                .address(partner.getAddress())
                .active(partner.isActive())
                .createdAt(partner.getCreatedAt())
                .updatedAt(partner.getUpdatedAt())
                .build();
    }
}
