package com.lgb.factoryflow.domain.partner.service;

import com.lgb.factoryflow.domain.partner.dto.PartnerCreateRequest;
import com.lgb.factoryflow.domain.partner.dto.PartnerResponse;
import com.lgb.factoryflow.domain.partner.dto.PartnerStatusUpdateRequest;
import com.lgb.factoryflow.domain.partner.dto.PartnerUpdateRequest;
import com.lgb.factoryflow.domain.partner.entity.Partner;
import com.lgb.factoryflow.domain.partner.entity.PartnerType;
import com.lgb.factoryflow.domain.partner.repository.PartnerRepository;
import com.lgb.factoryflow.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PartnerServiceTest {

    @Mock
    private PartnerRepository partnerRepository;

    @InjectMocks
    private PartnerService partnerService;

    @Test
    @DisplayName("거래처 등록 시 코드는 대문자로 정규화되어 저장된다")
    void createPartnerSuccess() {
        PartnerCreateRequest request = new PartnerCreateRequest(
                " ptn-sup-01 ",
                "대한골판지",
                PartnerType.SUPPLIER,
                "123-45-67890",
                "031-111-2222",
                "경기도 안산시"
        );

        given(partnerRepository.existsByPartnerCode("PTN-SUP-01")).willReturn(false);
        given(partnerRepository.save(any(Partner.class))).willAnswer(invocation -> invocation.getArgument(0));

        PartnerResponse response = partnerService.createPartner(request);

        assertThat(response.getPartnerCode()).isEqualTo("PTN-SUP-01");
        assertThat(response.getPartnerName()).isEqualTo("대한골판지");
        assertThat(response.getPartnerType()).isEqualTo(PartnerType.SUPPLIER);
        assertThat(response.getPartnerTypeDescription()).isEqualTo("매입처");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("중복된 거래처 코드로 등록하면 409 CONFLICT 예외가 발생한다")
    void createPartnerDuplicateThrowsConflict() {
        PartnerCreateRequest request = new PartnerCreateRequest(
                "PTN-CUS-01",
                "행복물류",
                PartnerType.CUSTOMER,
                null,
                null,
                null
        );

        given(partnerRepository.existsByPartnerCode("PTN-CUS-01")).willReturn(true);

        assertThatThrownBy(() -> partnerService.createPartner(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(partnerRepository, never()).save(any(Partner.class));
    }

    @Test
    @DisplayName("거래처 정보 수정 및 활성 상태 변경이 정상 반영된다")
    void updatePartnerAndStatusSuccess() {
        Partner partner = Partner.builder()
                .partnerCode("PTN-CUS-01")
                .partnerName("행복물류")
                .partnerType(PartnerType.CUSTOMER)
                .active(true)
                .build();

        given(partnerRepository.findById(1L)).willReturn(Optional.of(partner));

        PartnerResponse updated = partnerService.updatePartner(1L, new PartnerUpdateRequest(
                "행복물류(주)",
                PartnerType.BOTH,
                "222-33-44444",
                "02-555-6666",
                "서울시 구로구"
        ));
        assertThat(updated.getPartnerName()).isEqualTo("행복물류(주)");
        assertThat(updated.getPartnerType()).isEqualTo(PartnerType.BOTH);

        PartnerResponse deactivated = partnerService.updatePartnerStatus(1L, new PartnerStatusUpdateRequest(false));
        assertThat(deactivated.isActive()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 거래처 ID 조회 시 404 NOT_FOUND 예외가 발생한다")
    void getPartnerNotFoundThrowsException() {
        given(partnerRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> partnerService.getPartner(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
