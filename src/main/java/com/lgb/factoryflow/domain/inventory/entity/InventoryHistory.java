package com.lgb.factoryflow.domain.inventory.entity;

import com.lgb.factoryflow.domain.item.entity.Item;
import com.lgb.factoryflow.domain.partner.entity.Partner;
import com.lgb.factoryflow.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "inventory_histories")
public class InventoryHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    private Partner partner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InventoryTransactionType transactionType;

    @Column(nullable = false)
    private int beforeQuantity;

    @Column(nullable = false)
    private int changeQuantity;

    @Column(nullable = false)
    private int afterQuantity;

    @Column(length = 255)
    private String remark;

    @Builder
    public InventoryHistory(
            Item item,
            Partner partner,
            InventoryTransactionType transactionType,
            int beforeQuantity,
            int changeQuantity,
            int afterQuantity,
            String remark
    ) {
        this.item = item;
        this.partner = partner;
        this.transactionType = transactionType;
        this.beforeQuantity = beforeQuantity;
        this.changeQuantity = changeQuantity;
        this.afterQuantity = afterQuantity;
        this.remark = remark;
    }
}
