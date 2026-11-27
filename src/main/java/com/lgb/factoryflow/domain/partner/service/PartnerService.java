package com.lgb.factoryflow.domain.partner.service;

import com.lgb.factoryflow.domain.partner.dto.PartnerCreateRequest;
import com.lgb.factoryflow.domain.partner.dto.PartnerResponse;
import com.lgb.factoryflow.domain.partner.dto.PartnerStatusUpdateRequest;
import com.lgb.factoryflow.domain.partner.dto.PartnerUpdateRequest;
import com.lgb.factoryflow.domain.partner.entity.Partner;
import com.lgb.factoryflow.domain.partner.entity.PartnerType;
import com.lgb.factoryflow.domain.partner.repository.PartnerRepository;
import com.lgb.factoryflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartnerService {

    private final PartnerRepository partnerRepository;

    @Transactional
    public PartnerResponse createPartner(PartnerCreateRequest request) {
        String normalizedCode = request.getPartnerCode().trim().toUpperCase();
        if (partnerRepository.existsByPartnerCode(normalizedCode)) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 등록된 거래처 코드입니다: " + normalizedCode);
        }

        Partner savedPartner = partnerRepository.save(request.toEntity(normalizedCode));
        return PartnerResponse.from(savedPartner);
    }

    public List<PartnerResponse> getPartners(PartnerType partnerType, Boolean active) {
        List<Partner> partners;
        if (partnerType != null && active != null) {
            partners = partnerRepository.findByPartnerTypeAndActiveOrderByIdDesc(partnerType, active);
        } else if (partnerType != null) {
            partners = partnerRepository.findByPartnerTypeOrderByIdDesc(partnerType);
        } else if (active != null) {
            partners = partnerRepository.findByActiveOrderByIdDesc(active);
        } else {
            partners = partnerRepository.findAllByOrderByIdDesc();
        }
        return partners.stream()
                .map(PartnerResponse::from)
                .toList();
    }

    public PartnerResponse getPartner(Long id) {
        return PartnerResponse.from(findPartnerById(id));
    }

    @Transactional
    public PartnerResponse updatePartner(Long id, PartnerUpdateRequest request) {
        Partner partner = findPartnerById(id);
        partner.update(
                request.getPartnerName().trim(),
                request.getPartnerType(),
                trimToNull(request.getBusinessNumber()),
                trimToNull(request.getPhone()),
                trimToNull(request.getAddress())
        );
        return PartnerResponse.from(partner);
    }

    @Transactional
    public PartnerResponse updatePartnerStatus(Long id, PartnerStatusUpdateRequest request) {
        Partner partner = findPartnerById(id);
        partner.changeActive(request.getActive());
        return PartnerResponse.from(partner);
    }

    private Partner findPartnerById(Long id) {
        return partnerRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "거래처를 찾을 수 없습니다. id=" + id));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
