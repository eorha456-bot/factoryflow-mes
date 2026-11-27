package com.lgb.factoryflow.global.config;

import com.lgb.factoryflow.global.HealthController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@Import(WebConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=https://factoryflow.vercel.app, https://mes.example.com")
class WebConfigCorsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("환경변수로 지정한 운영 프론트엔드 도메인(공백 포함 쉼표 구분)의 CORS 요청을 허용한다")
    void configuredProductionOriginsAreAllowed() throws Exception {
        mockMvc.perform(options("/api/health")
                        .header("Origin", "https://mes.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://mes.example.com"));
    }

    @Test
    @DisplayName("허용 목록에 없는 외부 도메인의 CORS 요청은 차단한다")
    void unconfiguredOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/health")
                        .header("Origin", "https://evil.example.net")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
