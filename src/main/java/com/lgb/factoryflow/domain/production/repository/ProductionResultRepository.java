package com.lgb.factoryflow.domain.production.repository;

import com.lgb.factoryflow.domain.production.entity.ProductionResult;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductionResultRepository extends JpaRepository<ProductionResult, Long> {

    @EntityGraph(attributePaths = {"workOrder", "workOrder.item"})
    List<ProductionResult> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = {"workOrder", "workOrder.item"})
    List<ProductionResult> findByWorkOrderIdOrderByIdDesc(Long workOrderId);

    @EntityGraph(attributePaths = {"workOrder", "workOrder.item"})
    List<ProductionResult> findByCreatedAtBetweenOrderByIdDesc(LocalDateTime start, LocalDateTime end);
}
