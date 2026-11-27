package com.lgb.factoryflow.domain.partner.controller;

import com.lgb.factoryflow.domain.partner.dto.PartnerResponse;
import com.lgb.factoryflow.domain.partner.entity.PartnerType;
import com.lgb.factoryflow.domain.partner.service.PartnerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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

@WebMvcTest(PartnerController.class)
class PartnerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PartnerService partnerService;

    @Test
    @DisplayName("POST /api/partners 성공 시 201 Created를 반환한다")
    void createPartnerSuccess() throws Exception {
        PartnerResponse response = PartnerResponse.builder()
                .id(1L)
                .partnerCode("PTN-SUP-01")
                .partnerName("대한골판지")
                .partnerType(PartnerType.SUPPLIER)
                .partnerTypeDescription("매입처")
                .active(true)
                .build();

        given(partnerService.createPartner(any())).willReturn(response);

        String requestBody = """
                {
                  "partnerCode": "PTN-SUP-01",
                  "partnerName": "대한골판지",
                  "partnerType": "SUPPLIER"
                }
                """;

        mockMvc.perform(post("/api/partners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.partnerCode").value("PTN-SUP-01"));
    }

    @Test
    @DisplayName("POST /api/partners 필수값 누락 시 400 Bad Request를 반환한다")
    void createPartnerValidationFailure() throws Exception {
        mockMvc.perform(post("/api/partners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"partnerCode\": \"\", \"partnerName\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.partnerCode").exists())
                .andExpect(jsonPath("$.data.partnerType").exists());
    }

    @Test
    @DisplayName("GET /api/partners, PUT, PATCH 상태 변경이 정상 동작한다")
    void partnerQueryAndUpdateSuccess() throws Exception {
        PartnerResponse partner = PartnerResponse.builder()
                .id(1L)
                .partnerCode("PTN-CUS-01")
                .partnerName("행복물류")
                .partnerType(PartnerType.CUSTOMER)
                .partnerTypeDescription("납품처")
                .active(false)
                .build();

        given(partnerService.getPartners(PartnerType.CUSTOMER, true)).willReturn(List.of(partner));
        given(partnerService.updatePartner(eq(1L), any())).willReturn(partner);
        given(partnerService.updatePartnerStatus(eq(1L), any())).willReturn(partner);

        mockMvc.perform(get("/api/partners")
                        .param("partnerType", "CUSTOMER")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].partnerCode").value("PTN-CUS-01"));

        mockMvc.perform(put("/api/partners/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"partnerName\": \"행복물류\", \"partnerType\": \"CUSTOMER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.partnerName").value("행복물류"));

        mockMvc.perform(patch("/api/partners/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }
}
