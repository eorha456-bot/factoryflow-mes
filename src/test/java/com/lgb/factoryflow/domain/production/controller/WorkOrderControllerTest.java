package com.lgb.factoryflow.domain.production.controller;

import com.lgb.factoryflow.domain.production.dto.ProductionResultResponse;
import com.lgb.factoryflow.domain.production.dto.WorkOrderResponse;
import com.lgb.factoryflow.domain.production.entity.WorkOrderStatus;
import com.lgb.factoryflow.domain.production.service.ProductionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkOrderController.class)
class WorkOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductionService productionService;

    @Test
    @DisplayName("POST /api/work-orders 성공 시 201 Created를 반환한다")
    void createWorkOrderSuccess() throws Exception {
        WorkOrderResponse response = WorkOrderResponse.builder()
                .id(1L)
                .orderNumber("WO-20261127-001")
                .itemId(10L)
                .itemCode("BOX-003")
                .itemName("택배박스 3호")
                .unit("개")
                .targetQuantity(100)
                .producedGoodQuantity(0)
                .producedDefectQuantity(0)
                .progressRate(0.0)
                .status(WorkOrderStatus.WAITING)
                .statusDescription("대기")
                .plannedDate(LocalDate.of(2026, 11, 27))
                .build();

        given(productionService.createWorkOrder(any())).willReturn(response);

        String body = """
                {
                  "orderNumber": "WO-20261127-001",
                  "itemId": 10,
                  "targetQuantity": 100,
                  "plannedDate": "2026-11-27"
                }
                """;

        mockMvc.perform(post("/api/work-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").value("WO-20261127-001"));
    }

    @Test
    @DisplayName("POST /api/work-orders 필수값 누락 시 400 Bad Request를 반환한다")
    void createWorkOrderValidationFailure() throws Exception {
        mockMvc.perform(post("/api/work-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\": null, \"targetQuantity\": 0, \"plannedDate\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.itemId").exists())
                .andExpect(jsonPath("$.data.targetQuantity").exists())
                .andExpect(jsonPath("$.data.plannedDate").exists());
    }

    @Test
    @DisplayName("PATCH 상태 변경 및 POST 생산실적 등록 API가 정상 동작한다")
    void statusUpdateAndRecordResultSuccess() throws Exception {
        WorkOrderResponse inProgress = WorkOrderResponse.builder()
                .id(1L)
                .orderNumber("WO-20261127-001")
                .status(WorkOrderStatus.IN_PROGRESS)
                .statusDescription("작업중")
                .build();

        ProductionResultResponse result = ProductionResultResponse.builder()
                .id(5L)
                .workOrderId(1L)
                .orderNumber("WO-20261127-001")
                .goodQuantity(95)
                .defectQuantity(5)
                .totalQuantity(100)
                .cumulativeGoodQuantity(95)
                .cumulativeDefectQuantity(5)
                .workOrderStatus(WorkOrderStatus.IN_PROGRESS)
                .workerName("김작업")
                .build();

        given(productionService.updateWorkOrderStatus(eq(1L), any())).willReturn(inProgress);
        given(productionService.recordProductionResult(eq(1L), any())).willReturn(result);
        given(productionService.getProductionResultsByWorkOrder(1L)).willReturn(List.of(result));

        mockMvc.perform(patch("/api/work-orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        mockMvc.perform(post("/api/work-orders/1/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goodQuantity\": 95, \"defectQuantity\": 5, \"workerName\": \"김작업\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.goodQuantity").value(95))
                .andExpect(jsonPath("$.data.totalQuantity").value(100));

        mockMvc.perform(get("/api/work-orders/1/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].workerName").value("김작업"));
    }
}
