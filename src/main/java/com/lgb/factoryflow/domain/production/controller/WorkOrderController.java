package com.lgb.factoryflow.domain.production.controller;

import com.lgb.factoryflow.domain.production.dto.ProductionResultCreateRequest;
import com.lgb.factoryflow.domain.production.dto.ProductionResultResponse;
import com.lgb.factoryflow.domain.production.dto.WorkOrderCreateRequest;
import com.lgb.factoryflow.domain.production.dto.WorkOrderResponse;
import com.lgb.factoryflow.domain.production.dto.WorkOrderStatusUpdateRequest;
import com.lgb.factoryflow.domain.production.entity.WorkOrderStatus;
import com.lgb.factoryflow.domain.production.service.ProductionService;
import com.lgb.factoryflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WorkOrderController {

    private final ProductionService productionService;

    @PostMapping("/work-orders")
    public ResponseEntity<ApiResponse<WorkOrderResponse>> createWorkOrder(
            @Valid @RequestBody WorkOrderCreateRequest request
    ) {
        WorkOrderResponse response = productionService.createWorkOrder(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("작업지시가 등록되었습니다.", response));
    }

    @GetMapping("/work-orders")
    public ApiResponse<List<WorkOrderResponse>> getWorkOrders(
            @RequestParam(required = false) WorkOrderStatus status,
            @RequestParam(required = false) Long itemId
    ) {
        return ApiResponse.success(productionService.getWorkOrders(status, itemId));
    }

    @GetMapping("/work-orders/{id}")
    public ApiResponse<WorkOrderResponse> getWorkOrder(@PathVariable Long id) {
        return ApiResponse.success(productionService.getWorkOrder(id));
    }

    @PatchMapping("/work-orders/{id}/status")
    public ApiResponse<WorkOrderResponse> updateWorkOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody WorkOrderStatusUpdateRequest request
    ) {
        return ApiResponse.success(
                "작업지시 상태가 변경되었습니다.",
                productionService.updateWorkOrderStatus(id, request)
        );
    }

    @PostMapping("/work-orders/{workOrderId}/results")
    public ResponseEntity<ApiResponse<ProductionResultResponse>> recordProductionResult(
            @PathVariable Long workOrderId,
            @Valid @RequestBody ProductionResultCreateRequest request
    ) {
        ProductionResultResponse response = productionService.recordProductionResult(workOrderId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("생산실적이 등록되고 재고가 반영되었습니다.", response));
    }

    @GetMapping("/work-orders/{workOrderId}/results")
    public ApiResponse<List<ProductionResultResponse>> getProductionResultsByWorkOrder(
            @PathVariable Long workOrderId
    ) {
        return ApiResponse.success(productionService.getProductionResultsByWorkOrder(workOrderId));
    }

    @GetMapping("/production-results")
    public ApiResponse<List<ProductionResultResponse>> getAllProductionResults() {
        return ApiResponse.success(productionService.getAllProductionResults());
    }
}
