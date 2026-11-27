package com.lgb.factoryflow.domain.inventory.repository;

import com.lgb.factoryflow.domain.inventory.entity.InventoryHistory;
import com.lgb.factoryflow.domain.inventory.entity.InventoryTransactionType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, Long> {

    @EntityGraph(attributePaths = {"item", "partner"})
    List<InventoryHistory> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = {"item", "partner"})
    List<InventoryHistory> findByItemIdOrderByIdDesc(Long itemId);

    @EntityGraph(attributePaths = {"item", "partner"})
    List<InventoryHistory> findByTransactionTypeOrderByIdDesc(InventoryTransactionType transactionType);

    @EntityGraph(attributePaths = {"item", "partner"})
    List<InventoryHistory> findByItemIdAndTransactionTypeOrderByIdDesc(
            Long itemId,
            InventoryTransactionType transactionType
    );
}
