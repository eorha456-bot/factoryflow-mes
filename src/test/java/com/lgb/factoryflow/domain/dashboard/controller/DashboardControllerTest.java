package com.lgb.factoryflow.domain.dashboard.controller;

import com.lgb.factoryflow.domain.dashboard.dto.DashboardSummaryResponse;
import com.lgb.factoryflow.domain.dashboard.service.DashboardService;
import com.lgb.factoryflow.domain.inventory.dto.InventoryResponse;
import com.lgb.factoryflow.domain.item.entity.ItemType;
import com.lgb.factoryflow.domain.production.dto.WorkOrderResponse;
import com.lgb.factoryflow.domain.production.entity.WorkOrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    @DisplayName("GET /api/dashboard 호출 시 대시보드 요약 정보를 반환한다")
    void getDashboardSummarySuccess() throws Exception {
        DashboardSummaryResponse response = DashboardSummaryResponse.builder()
                .baseDate(LocalDate.of(2026, 11, 27))
                .waitingWorkOrderCount(2)
                .inProgressWorkOrderCount(1)
                .completedWorkOrderCount(4)
                .todayResultCount(3)
                .todayGoodQuantity(190)
                .todayDefectQuantity(10)
                .todayTotalQuantity(200)
                .todayDefectRate(5.0)
                .belowSafetyStockCount(1)
                .belowSafetyStockItems(List.of(
                        InventoryResponse.builder()
                                .itemId(1L)
                                .itemCode("RAW-001")
                                .itemName("AB골 원단")
                                .itemType(ItemType.RAW_MATERIAL)
                                .itemTypeDescription("원재료")
                                .unit("매")
                                .quantity(15)
                                .safetyStock(50)
                                .belowSafetyStock(true)
                                .active(true)
                                .build()
                ))
                .inProgressWorkOrders(List.of(
                        WorkOrderResponse.builder()
                                .id(10L)
                                .orderNumber("WO-20261127-001")
                                .itemName("택배박스 3호")
                                .status(WorkOrderStatus.IN_PROGRESS)
                                .statusDescription("작업중")
                                .progressRate(50.0)
                                .build()
                ))
                .build();

        given(dashboardService.getSummary(null)).willReturn(response);

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.baseDate").value("2026-11-27"))
                .andExpect(jsonPath("$.data.waitingWorkOrderCount").value(2))
                .andExpect(jsonPath("$.data.inProgressWorkOrderCount").value(1))
                .andExpect(jsonPath("$.data.completedWorkOrderCount").value(4))
                .andExpect(jsonPath("$.data.todayGoodQuantity").value(190))
                .andExpect(jsonPath("$.data.todayDefectQuantity").value(10))
                .andExpect(jsonPath("$.data.todayTotalQuantity").value(200))
                .andExpect(jsonPath("$.data.todayDefectRate").value(5.0))
                .andExpect(jsonPath("$.data.belowSafetyStockCount").value(1))
                .andExpect(jsonPath("$.data.belowSafetyStockItems[0].itemCode").value("RAW-001"))
                .andExpect(jsonPath("$.data.inProgressWorkOrders[0].orderNumber").value("WO-20261127-001"));
    }

    @Test
    @DisplayName("GET /api/dashboard?date=2026-11-27 지정 날짜 파라미터가 정상 전달된다")
    void getDashboardSummaryWithDateParam() throws Exception {
        LocalDate targetDate = LocalDate.of(2026, 11, 27);
        DashboardSummaryResponse response = DashboardSummaryResponse.builder()
                .baseDate(targetDate)
                .belowSafetyStockItems(List.of())
                .inProgressWorkOrders(List.of())
                .build();

        given(dashboardService.getSummary(targetDate)).willReturn(response);

        mockMvc.perform(get("/api/dashboard").param("date", "2026-11-27"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.baseDate").value("2026-11-27"));
    }

    @Test
    @DisplayName("GET /api/dashboard 잘못된 date 형식 전달 시 400 Bad Request를 반환한다")
    void getDashboardSummaryInvalidDateReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/dashboard").param("date", "2026-13-99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
