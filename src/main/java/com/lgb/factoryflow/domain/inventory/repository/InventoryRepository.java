package com.lgb.factoryflow.domain.inventory.repository;

import com.lgb.factoryflow.domain.inventory.entity.Inventory;
import com.lgb.factoryflow.domain.item.entity.ItemType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @EntityGraph(attributePaths = {"item"})
    Optional<Inventory> findByItemId(Long itemId);

    @EntityGraph(attributePaths = {"item"})
    List<Inventory> findAllByOrderByItemIdAsc();

    @EntityGraph(attributePaths = {"item"})
    List<Inventory> findByItemItemTypeOrderByItemIdAsc(ItemType itemType);
}
