package com.lgb.factoryflow.domain.inventory.entity;

import com.lgb.factoryflow.domain.item.entity.Item;
import com.lgb.factoryflow.global.entity.BaseTimeEntity;
import com.lgb.factoryflow.global.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "inventories")
public class Inventory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false, unique = true)
    private Item item;

    @Column(nullable = false)
    private int quantity;

    @Version
    private Long version;

    @Builder
    public Inventory(Item item, int quantity) {
        if (quantity < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "재고 수량은 0 미만일 수 없습니다.");
        }
        this.item = item;
        this.quantity = quantity;
    }

    public static Inventory createEmpty(Item item) {
        return Inventory.builder()
                .item(item)
                .quantity(0)
                .build();
    }

    public void increase(int amount) {
        if (amount <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "입고 수량은 1 이상이어야 합니다.");
        }
        this.quantity += amount;
    }

    public void decrease(int amount) {
        if (amount <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "출고 수량은 1 이상이어야 합니다.");
        }
        if (this.quantity < amount) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    String.format("재고가 부족합니다. 품목=%s, 현재고=%d, 요청수량=%d",
                            item.getItemName(), this.quantity, amount)
            );
        }
        this.quantity -= amount;
    }

    public int adjustTo(int targetQuantity) {
        if (targetQuantity < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "조정 후 재고 수량은 0 이상이어야 합니다.");
        }
        int delta = targetQuantity - this.quantity;
        if (delta == 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "변경할 재고 수량이 현재고와 동일합니다.");
        }
        this.quantity = targetQuantity;
        return delta;
    }

    public boolean isBelowSafetyStock() {
        return this.quantity < this.item.getSafetyStock();
    }
}
