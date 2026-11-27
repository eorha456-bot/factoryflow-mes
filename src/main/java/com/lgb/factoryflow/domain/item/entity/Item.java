package com.lgb.factoryflow.domain.item.entity;

import com.lgb.factoryflow.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "items")
public class Item extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String itemCode;

    @Column(nullable = false, length = 100)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ItemType itemType;

    @Column(length = 100)
    private String spec;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(nullable = false)
    private int safetyStock;

    @Column(nullable = false)
    private boolean active;

    @Builder
    public Item(
            String itemCode,
            String itemName,
            ItemType itemType,
            String spec,
            String unit,
            int safetyStock,
            Boolean active
    ) {
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.itemType = itemType;
        this.spec = spec;
        this.unit = unit;
        this.safetyStock = safetyStock;
        this.active = active == null || active;
    }

    public void update(String itemName, ItemType itemType, String spec, String unit, int safetyStock) {
        this.itemName = itemName;
        this.itemType = itemType;
        this.spec = spec;
        this.unit = unit;
        this.safetyStock = safetyStock;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }
}
