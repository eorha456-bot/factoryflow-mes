package com.lgb.factoryflow.domain.partner.dto;

import com.lgb.factoryflow.domain.partner.entity.Partner;
import com.lgb.factoryflow.domain.partner.entity.PartnerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PartnerCreateRequest {

    @NotBlank(message = "거래처 코드는 필수입니다.")
    @Size(max = 50, message = "거래처 코드는 50자 이하로 입력해 주세요.")
    private String partnerCode;

    @NotBlank(message = "거래처명은 필수입니다.")
    @Size(max = 100, message = "거래처명은 100자 이하로 입력해 주세요.")
    private String partnerName;

    @NotNull(message = "거래처 구분(SUPPLIER, CUSTOMER, BOTH)은 필수입니다.")
    private PartnerType partnerType;

    @Size(max = 30, message = "사업자번호는 30자 이하로 입력해 주세요.")
    private String businessNumber;

    @Size(max = 30, message = "연락처는 30자 이하로 입력해 주세요.")
    private String phone;

    @Size(max = 200, message = "주소는 200자 이하로 입력해 주세요.")
    private String address;

    public PartnerCreateRequest(
            String partnerCode,
            String partnerName,
            PartnerType partnerType,
            String businessNumber,
            String phone,
            String address
    ) {
        this.partnerCode = partnerCode;
        this.partnerName = partnerName;
        this.partnerType = partnerType;
        this.businessNumber = businessNumber;
        this.phone = phone;
        this.address = address;
    }

    public Partner toEntity(String normalizedCode) {
        return Partner.builder()
                .partnerCode(normalizedCode)
                .partnerName(partnerName.trim())
                .partnerType(partnerType)
                .businessNumber(trimToNull(businessNumber))
                .phone(trimToNull(phone))
                .address(trimToNull(address))
                .active(true)
                .build();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
