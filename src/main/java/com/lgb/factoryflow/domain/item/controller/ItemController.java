package com.lgb.factoryflow.domain.item.controller;

import com.lgb.factoryflow.domain.item.dto.ItemCreateRequest;
import com.lgb.factoryflow.domain.item.dto.ItemResponse;
import com.lgb.factoryflow.domain.item.dto.ItemStatusUpdateRequest;
import com.lgb.factoryflow.domain.item.dto.ItemUpdateRequest;
import com.lgb.factoryflow.domain.item.entity.ItemType;
import com.lgb.factoryflow.domain.item.service.ItemService;
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
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    public ResponseEntity<ApiResponse<ItemResponse>> createItem(@Valid @RequestBody ItemCreateRequest request) {
        ItemResponse response = itemService.createItem(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("품목이 등록되었습니다.", response));
    }

    @GetMapping
    public ApiResponse<List<ItemResponse>> getItems(
            @RequestParam(required = false) ItemType itemType,
            @RequestParam(required = false) Boolean active
    ) {
        return ApiResponse.success(itemService.getItems(itemType, active));
    }

    @GetMapping("/{id}")
    public ApiResponse<ItemResponse> getItem(@PathVariable Long id) {
        return ApiResponse.success(itemService.getItem(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ItemResponse> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody ItemUpdateRequest request
    ) {
        return ApiResponse.success("품목 정보가 수정되었습니다.", itemService.updateItem(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<ItemResponse> updateItemStatus(
            @PathVariable Long id,
            @Valid @RequestBody ItemStatusUpdateRequest request
    ) {
        return ApiResponse.success("품목 사용 상태가 변경되었습니다.", itemService.updateItemStatus(id, request));
    }
}
