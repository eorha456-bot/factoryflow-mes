package com.lgb.factoryflow.domain.item.controller;

import com.lgb.factoryflow.domain.item.dto.ItemResponse;
import com.lgb.factoryflow.domain.item.entity.ItemType;
import com.lgb.factoryflow.domain.item.service.ItemService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    @Test
    @DisplayName("POST /api/items 성공 시 201 Created와 생성된 품목을 반환한다")
    void createItemSuccess() throws Exception {
        ItemResponse response = ItemResponse.builder()
                .id(1L)
                .itemCode("RAW-AB-01")
                .itemName("AB골 원단")
                .itemType(ItemType.RAW_MATERIAL)
                .itemTypeDescription("원재료")
                .spec("1200x1000")
                .unit("매")
                .safetyStock(100)
                .active(true)
                .build();

        given(itemService.createItem(any())).willReturn(response);

        String requestBody = """
                {
                  "itemCode": "RAW-AB-01",
                  "itemName": "AB골 원단",
                  "itemType": "RAW_MATERIAL",
                  "spec": "1200x1000",
                  "unit": "매",
                  "safetyStock": 100
                }
                """;

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.itemCode").value("RAW-AB-01"));
    }

    @Test
    @DisplayName("POST /api/items 필수값 누락 및 음수 안전재고 입력 시 400 Bad Request를 반환한다")
    void createItemValidationFailure() throws Exception {
        String invalidBody = """
                {
                  "itemCode": "",
                  "itemName": "",
                  "itemType": null,
                  "unit": "",
                  "safetyStock": -5
                }
                """;

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.itemCode").exists())
                .andExpect(jsonPath("$.data.safetyStock").exists());
    }

    @Test
    @DisplayName("GET /api/items 필터 조건으로 품목 목록을 조회한다")
    void getItemsSuccess() throws Exception {
        ItemResponse item = ItemResponse.builder()
                .id(1L)
                .itemCode("BOX-003")
                .itemName("택배박스 3호")
                .itemType(ItemType.FINISHED_GOOD)
                .itemTypeDescription("완제품")
                .spec("300x200x150")
                .unit("개")
                .safetyStock(50)
                .active(true)
                .build();

        given(itemService.getItems(ItemType.FINISHED_GOOD, true)).willReturn(List.of(item));

        mockMvc.perform(get("/api/items")
                        .param("itemType", "FINISHED_GOOD")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].itemCode").value("BOX-003"));
    }

    @Test
    @DisplayName("PUT /api/items/{id} 와 PATCH /api/items/{id}/status 가 정상 동작한다")
    void updateItemAndStatusSuccess() throws Exception {
        ItemResponse updated = ItemResponse.builder()
                .id(1L)
                .itemCode("BOX-003")
                .itemName("택배박스 3호(수정)")
                .itemType(ItemType.FINISHED_GOOD)
                .itemTypeDescription("완제품")
                .spec("310x210x160")
                .unit("개")
                .safetyStock(80)
                .active(false)
                .build();

        given(itemService.updateItem(eq(1L), any())).willReturn(updated);
        given(itemService.updateItemStatus(eq(1L), any())).willReturn(updated);

        String updateBody = """
                {
                  "itemName": "택배박스 3호(수정)",
                  "itemType": "FINISHED_GOOD",
                  "spec": "310x210x160",
                  "unit": "개",
                  "safetyStock": 80
                }
                """;

        mockMvc.perform(put("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemName").value("택배박스 3호(수정)"));

        mockMvc.perform(patch("/api/items/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    @DisplayName("중복 코드 등 BusinessException 발생 시 지정한 상태코드와 JSON 에러를 반환한다")
    void businessExceptionHandled() throws Exception {
        given(itemService.getItem(999L))
                .willThrow(new BusinessException(HttpStatus.NOT_FOUND, "품목을 찾을 수 없습니다. id=999"));

        mockMvc.perform(get("/api/items/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("품목을 찾을 수 없습니다. id=999"));
    }
}
