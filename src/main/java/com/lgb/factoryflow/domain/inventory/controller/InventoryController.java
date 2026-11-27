package com.lgb.factoryflow.domain.inventory.controller;

import com.lgb.factoryflow.domain.inventory.dto.InventoryAdjustRequest;
import com.lgb.factoryflow.domain.inventory.dto.InventoryHistoryResponse;
import com.lgb.factoryflow.domain.inventory.dto.InventoryInboundRequest;
import com.lgb.factoryflow.domain.inventory.dto.InventoryOutboundRequest;
import com.lgb.factoryflow.domain.inventory.dto.InventoryResponse;
import com.lgb.factoryflow.domain.inventory.entity.InventoryTransactionType;
import com.lgb.factoryflow.domain.inventory.service.InventoryService;
import com.lgb.factoryflow.domain.item.entity.ItemType;
import com.lgb.factoryflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ApiResponse<List<InventoryResponse>> getInventories(
            @RequestParam(required = false) ItemType itemType,
            @RequestParam(required = false) Boolean belowSafetyOnly
    ) {
        return ApiResponse.success(inventoryService.getInventories(itemType, belowSafetyOnly));
    }

    @GetMapping("/items/{itemId}")
    public ApiResponse<InventoryResponse> getInventoryByItemId(@PathVariable Long itemId) {
        return ApiResponse.success(inventoryService.getInventoryByItemId(itemId));
    }

    @GetMapping("/histories")
    public ApiResponse<List<InventoryHistoryResponse>> getHistories(
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) InventoryTransactionType transactionType
    ) {
        return ApiResponse.success(inventoryService.getHistories(itemId, transactionType));
    }

    @PostMapping("/inbound")
    public ResponseEntity<ApiResponse<InventoryHistoryResponse>> inbound(
            @Valid @RequestBody InventoryInboundRequest request
    ) {
        InventoryHistoryResponse response = inventoryService.inbound(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("입고 처리가 완료되었습니다.", response));
    }

    @PostMapping("/outbound")
    public ResponseEntity<ApiResponse<InventoryHistoryResponse>> outbound(
            @Valid @RequestBody InventoryOutboundRequest request
    ) {
        InventoryHistoryResponse response = inventoryService.outbound(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("출고 처리가 완료되었습니다.", response));
    }

    @PostMapping("/adjust")
    public ResponseEntity<ApiResponse<InventoryHistoryResponse>> adjust(
            @Valid @RequestBody InventoryAdjustRequest request
    ) {
        InventoryHistoryResponse response = inventoryService.adjust(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("재고 조정이 완료되었습니다.", response));
    }
}
