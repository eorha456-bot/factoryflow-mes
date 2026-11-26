package com.lgb.factoryflow.global;

import com.lgb.factoryflow.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(
                "FactoryFlow API 서버가 정상 동작 중입니다.",
                Map.of(
                        "status", "UP",
                        "service", "factoryflow"
                )
        );
    }
}
