package com.lgb.factoryflow.domain.inventory.controller;

import com.lgb.factoryflow.domain.inventory.dto.InventoryHistoryResponse;
import com.lgb.factoryflow.domain.inventory.dto.InventoryResponse;
import com.lgb.factoryflow.domain.inventory.entity.InventoryTransactionType;
import com.lgb.factoryflow.domain.inventory.service.InventoryService;
import com.lgb.factoryflow.domain.item.entity.ItemType;
import com.lgb.factoryflow.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @Test
    @DisplayName("GET /api/inventories 와 /histories 가 정상 조회된다")
    void getInventoriesAndHistoriesSuccess() throws Exception {
        InventoryResponse inventory = InventoryResponse.builder()
                .inventoryId(1L)
                .itemId(1L)
                .itemCode("RAW-AB-01")
                .itemName("AB골 원단")
                .itemType(ItemType.RAW_MATERIAL)
                .itemTypeDescription("원재료")
                .unit("매")
                .quantity(30)
                .safetyStock(50)
                .belowSafetyStock(true)
                .build();

        InventoryHistoryResponse history = InventoryHistoryResponse.builder()
                .id(1L)
                .itemId(1L)
                .itemCode("RAW-AB-01")
                .itemName("AB골 원단")
                .unit("매")
                .transactionType(InventoryTransactionType.INBOUND)
                .transactionTypeDescription("입고")
                .beforeQuantity(0)
                .changeQuantity(30)
                .afterQuantity(30)
                .build();

        given(inventoryService.getInventories(ItemType.RAW_MATERIAL, true)).willReturn(List.of(inventory));
        given(inventoryService.getHistories(1L, InventoryTransactionType.INBOUND)).willReturn(List.of(history));

        mockMvc.perform(get("/api/inventories")
                        .param("itemType", "RAW_MATERIAL")
                        .param("belowSafetyOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].belowSafetyStock").value(true));

        mockMvc.perform(get("/api/inventories/histories")
                        .param("itemId", "1")
                        .param("transactionType", "INBOUND"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].transactionType").value("INBOUND"))
                .andExpect(jsonPath("$.data[0].afterQuantity").value(30));
    }

    @Test
    @DisplayName("POST /api/inventories/inbound 성공 시 201 Created를 반환한다")
    void inboundSuccess() throws Exception {
        InventoryHistoryResponse response = InventoryHistoryResponse.builder()
                .id(1L)
                .itemId(1L)
                .itemCode("RAW-AB-01")
                .itemName("AB골 원단")
                .unit("매")
                .transactionType(InventoryTransactionType.INBOUND)
                .transactionTypeDescription("입고")
                .beforeQuantity(0)
                .changeQuantity(50)
                .afterQuantity(50)
                .build();

        given(inventoryService.inbound(any())).willReturn(response);

        mockMvc.perform(post("/api/inventories/inbound")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\": 1, \"partnerId\": 10, \"quantity\": 50, \"remark\": \"정기 입고\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.afterQuantity").value(50));
    }

    @Test
    @DisplayName("POST /api/inventories/inbound 수량이 0 이하이면 400 Bad Request를 반환한다")
    void inboundValidationFailure() throws Exception {
        mockMvc.perform(post("/api/inventories/inbound")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\": null, \"quantity\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.itemId").exists())
                .andExpect(jsonPath("$.data.quantity").exists());
    }

    @Test
    @DisplayName("POST /api/inventories/outbound 재고 부족 시 400 Bad Request와 에러 메시지를 반환한다")
    void outboundInsufficientStock() throws Exception {
        given(inventoryService.outbound(any()))
                .willThrow(new BusinessException(HttpStatus.BAD_REQUEST, "재고가 부족합니다."));

        mockMvc.perform(post("/api/inventories/outbound")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\": 1, \"quantity\": 999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("재고가 부족합니다."));
    }

    @Test
    @DisplayName("POST /api/inventories/adjust 사유 누락 시 400, 정상 입력 시 201을 반환한다")
    void adjustValidationAndSuccess() throws Exception {
        mockMvc.perform(post("/api/inventories/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\": 1, \"targetQuantity\": 10, \"remark\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.remark").exists());

        InventoryHistoryResponse response = InventoryHistoryResponse.builder()
                .id(2L)
                .itemId(1L)
                .itemCode("RAW-AB-01")
                .itemName("AB골 원단")
                .unit("매")
                .transactionType(InventoryTransactionType.ADJUST)
                .transactionTypeDescription("재고조정")
                .beforeQuantity(50)
                .changeQuantity(-8)
                .afterQuantity(42)
                .remark("실사 반영")
                .build();

        given(inventoryService.adjust(any())).willReturn(response);

        mockMvc.perform(post("/api/inventories/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\": 1, \"targetQuantity\": 42, \"remark\": \"실사 반영\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.changeQuantity").value(-8));
    }

    @Test
    @DisplayName("잘못된 Enum 쿼리 파라미터 전달 시 400 Bad Request를 반환한다")
    void invalidEnumQueryParamReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/inventories")
                        .param("itemType", "INVALID_TYPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
