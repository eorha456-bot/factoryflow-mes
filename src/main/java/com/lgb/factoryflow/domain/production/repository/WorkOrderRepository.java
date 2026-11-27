package com.lgb.factoryflow.domain.production.repository;

import com.lgb.factoryflow.domain.production.entity.WorkOrder;
import com.lgb.factoryflow.domain.production.entity.WorkOrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    boolean existsByOrderNumber(String orderNumber);

    @EntityGraph(attributePaths = {"item"})
    Optional<WorkOrder> findWithItemById(Long id);

    @EntityGraph(attributePaths = {"item"})
    List<WorkOrder> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = {"item"})
    List<WorkOrder> findByStatusOrderByIdDesc(WorkOrderStatus status);

    @EntityGraph(attributePaths = {"item"})
    List<WorkOrder> findByItemIdOrderByIdDesc(Long itemId);

    @EntityGraph(attributePaths = {"item"})
    List<WorkOrder> findByStatusAndItemIdOrderByIdDesc(WorkOrderStatus status, Long itemId);

    long countByStatus(WorkOrderStatus status);
}
