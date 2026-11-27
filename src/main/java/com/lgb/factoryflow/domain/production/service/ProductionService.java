package com.lgb.factoryflow.domain.production.service;

import com.lgb.factoryflow.domain.bom.entity.Bom;
import com.lgb.factoryflow.domain.bom.repository.BomRepository;
import com.lgb.factoryflow.domain.inventory.service.InventoryService;
import com.lgb.factoryflow.domain.item.entity.Item;
import com.lgb.factoryflow.domain.item.entity.ItemType;
import com.lgb.factoryflow.domain.item.repository.ItemRepository;
import com.lgb.factoryflow.domain.production.dto.ProductionResultCreateRequest;
import com.lgb.factoryflow.domain.production.dto.ProductionResultResponse;
import com.lgb.factoryflow.domain.production.dto.WorkOrderCreateRequest;
import com.lgb.factoryflow.domain.production.dto.WorkOrderResponse;
import com.lgb.factoryflow.domain.production.dto.WorkOrderStatusUpdateRequest;
import com.lgb.factoryflow.domain.production.entity.ProductionResult;
import com.lgb.factoryflow.domain.production.entity.WorkOrder;
import com.lgb.factoryflow.domain.production.entity.WorkOrderStatus;
import com.lgb.factoryflow.domain.production.repository.ProductionResultRepository;
import com.lgb.factoryflow.domain.production.repository.WorkOrderRepository;
import com.lgb.factoryflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductionService {

    private static final DateTimeFormatter ORDER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final WorkOrderRepository workOrderRepository;
    private final ProductionResultRepository productionResultRepository;
    private final ItemRepository itemRepository;
    private final BomRepository bomRepository;
    private final InventoryService inventoryService;

    @Transactional
    public WorkOrderResponse createWorkOrder(WorkOrderCreateRequest request) {
        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "품목을 찾을 수 없습니다. id=" + request.getItemId()));

        if (item.getItemType() != ItemType.FINISHED_GOOD) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "작업지시는 완제품(FINISHED_GOOD) 품목으로만 등록할 수 있습니다.");
        }
        if (!item.isActive()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "비활성화된 품목으로는 작업지시를 등록할 수 없습니다.");
        }

        String orderNumber = resolveOrderNumber(request.getOrderNumber());
        if (workOrderRepository.existsByOrderNumber(orderNumber)) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 등록된 작업지시 번호입니다: " + orderNumber);
        }

        WorkOrder saved = workOrderRepository.save(WorkOrder.builder()
                .orderNumber(orderNumber)
                .item(item)
                .targetQuantity(request.getTargetQuantity())
                .plannedDate(request.getPlannedDate())
                .remark(trimToNull(request.getRemark()))
                .build());

        return WorkOrderResponse.from(saved);
    }

    public List<WorkOrderResponse> getWorkOrders(WorkOrderStatus status, Long itemId) {
        List<WorkOrder> workOrders;
        if (status != null && itemId != null) {
            workOrders = workOrderRepository.findByStatusAndItemIdOrderByIdDesc(status, itemId);
        } else if (status != null) {
            workOrders = workOrderRepository.findByStatusOrderByIdDesc(status);
        } else if (itemId != null) {
            workOrders = workOrderRepository.findByItemIdOrderByIdDesc(itemId);
        } else {
            workOrders = workOrderRepository.findAllByOrderByIdDesc();
        }
        return workOrders.stream()
                .map(WorkOrderResponse::from)
                .toList();
    }

    public WorkOrderResponse getWorkOrder(Long id) {
        return WorkOrderResponse.from(findWorkOrderById(id));
    }

    @Transactional
    public WorkOrderResponse updateWorkOrderStatus(Long id, WorkOrderStatusUpdateRequest request) {
        WorkOrder workOrder = findWorkOrderById(id);
        workOrder.changeStatus(request.getStatus());
        return WorkOrderResponse.from(workOrder);
    }

    @Transactional
    public ProductionResultResponse recordProductionResult(Long workOrderId, ProductionResultCreateRequest request) {
        WorkOrder workOrder = findWorkOrderById(workOrderId);
        int goodQuantity = request.getGoodQuantity();
        int defectQuantity = request.getDefectQuantity();
        int totalProduced = goodQuantity + defectQuantity;

        workOrder.recordProduction(goodQuantity, defectQuantity);

        Item finishedItem = workOrder.getItem();
        List<Bom> boms = bomRepository.findByFinishedItemIdOrderByIdAsc(finishedItem.getId());
        for (Bom bom : boms) {
            int consumedRawQuantity = Math.multiplyExact(bom.getRequiredQuantity(), totalProduced);
            inventoryService.decreaseForProduction(
                    bom.getRawMaterial(),
                    consumedRawQuantity,
                    "작업지시 " + workOrder.getOrderNumber() + " 생산 투입"
            );
        }

        if (goodQuantity > 0) {
            inventoryService.increaseForProduction(
                    finishedItem,
                    goodQuantity,
                    "작업지시 " + workOrder.getOrderNumber() + " 생산 완료"
            );
        }

        ProductionResult savedResult = productionResultRepository.save(ProductionResult.builder()
                .workOrder(workOrder)
                .goodQuantity(goodQuantity)
                .defectQuantity(defectQuantity)
                .workerName(trimToNull(request.getWorkerName()))
                .remark(trimToNull(request.getRemark()))
                .build());

        return ProductionResultResponse.from(savedResult);
    }

    public List<ProductionResultResponse> getProductionResultsByWorkOrder(Long workOrderId) {
        findWorkOrderById(workOrderId);
        return productionResultRepository.findByWorkOrderIdOrderByIdDesc(workOrderId).stream()
                .map(ProductionResultResponse::from)
                .toList();
    }

    public List<ProductionResultResponse> getAllProductionResults() {
        return productionResultRepository.findAllByOrderByIdDesc().stream()
                .map(ProductionResultResponse::from)
                .toList();
    }

    private WorkOrder findWorkOrderById(Long id) {
        return workOrderRepository.findWithItemById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "작업지시를 찾을 수 없습니다. id=" + id));
    }

    private String resolveOrderNumber(String requestedOrderNumber) {
        if (requestedOrderNumber != null && !requestedOrderNumber.isBlank()) {
            return requestedOrderNumber.trim().toUpperCase();
        }
        String datePart = LocalDateTime.now().format(ORDER_DATE_FORMAT);
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "WO-" + datePart + "-" + randomSuffix;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
