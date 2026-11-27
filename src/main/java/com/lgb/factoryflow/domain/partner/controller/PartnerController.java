package com.lgb.factoryflow.domain.partner.controller;

import com.lgb.factoryflow.domain.partner.dto.PartnerCreateRequest;
import com.lgb.factoryflow.domain.partner.dto.PartnerResponse;
import com.lgb.factoryflow.domain.partner.dto.PartnerStatusUpdateRequest;
import com.lgb.factoryflow.domain.partner.dto.PartnerUpdateRequest;
import com.lgb.factoryflow.domain.partner.entity.PartnerType;
import com.lgb.factoryflow.domain.partner.service.PartnerService;
import com.lgb.factoryflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/partners")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    @PostMapping
    public ResponseEntity<ApiResponse<PartnerResponse>> createPartner(
            @Valid @RequestBody PartnerCreateRequest request
    ) {
        PartnerResponse response = partnerService.createPartner(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("거래처가 등록되었습니다.", response));
    }

    @GetMapping
    public ApiResponse<List<PartnerResponse>> getPartners(
            @RequestParam(required = false) PartnerType partnerType,
            @RequestParam(required = false) Boolean active
    ) {
        return ApiResponse.success(partnerService.getPartners(partnerType, active));
    }

    @GetMapping("/{id}")
    public ApiResponse<PartnerResponse> getPartner(@PathVariable Long id) {
        return ApiResponse.success(partnerService.getPartner(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<PartnerResponse> updatePartner(
            @PathVariable Long id,
            @Valid @RequestBody PartnerUpdateRequest request
    ) {
        return ApiResponse.success("거래처 정보가 수정되었습니다.", partnerService.updatePartner(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<PartnerResponse> updatePartnerStatus(
            @PathVariable Long id,
            @Valid @RequestBody PartnerStatusUpdateRequest request
    ) {
        return ApiResponse.success("거래처 사용 상태가 변경되었습니다.", partnerService.updatePartnerStatus(id, request));
    }
}
