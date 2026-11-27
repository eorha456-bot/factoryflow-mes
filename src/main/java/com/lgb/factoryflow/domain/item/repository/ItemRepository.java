package com.lgb.factoryflow.domain.item.repository;

import com.lgb.factoryflow.domain.item.entity.Item;
import com.lgb.factoryflow.domain.item.entity.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    boolean existsByItemCode(String itemCode);

    List<Item> findAllByOrderByIdDesc();

    List<Item> findByItemTypeOrderByIdDesc(ItemType itemType);

    List<Item> findByActiveOrderByIdDesc(boolean active);

    List<Item> findByItemTypeAndActiveOrderByIdDesc(ItemType itemType, boolean active);
}
