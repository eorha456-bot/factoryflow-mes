package com.lgb.factoryflow.domain.bom.controller;

import com.lgb.factoryflow.domain.bom.dto.BomCreateRequest;
import com.lgb.factoryflow.domain.bom.dto.BomResponse;
import com.lgb.factoryflow.domain.bom.dto.BomUpdateRequest;
import com.lgb.factoryflow.domain.bom.service.BomService;
import com.lgb.factoryflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/boms")
@RequiredArgsConstructor
public class BomController {

    private final BomService bomService;

    @PostMapping
    public ResponseEntity<ApiResponse<BomResponse>> createBom(@Valid @RequestBody BomCreateRequest request) {
        BomResponse response = bomService.createBom(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("BOM이 등록되었습니다.", response));
    }

    @GetMapping
    public ApiResponse<List<BomResponse>> getBoms(@RequestParam(required = false) Long finishedItemId) {
        return ApiResponse.success(bomService.getBoms(finishedItemId));
    }

    @PutMapping("/{id}")
    public ApiResponse<BomResponse> updateBom(
            @PathVariable Long id,
            @Valid @RequestBody BomUpdateRequest request
    ) {
        return ApiResponse.success("BOM 소요 수량이 수정되었습니다.", bomService.updateBom(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteBom(@PathVariable Long id) {
        bomService.deleteBom(id);
        return ApiResponse.success("BOM이 삭제되었습니다.", null);
    }
}
