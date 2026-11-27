package com.lgb.factoryflow.domain.bom.entity;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "boms",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_boms_finished_raw",
                columnNames = {"finished_item_id", "raw_material_id"}
        )
)
public class Bom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "finished_item_id", nullable = false)
    private Item finishedItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "raw_material_id", nullable = false)
    private Item rawMaterial;

    @Column(nullable = false)
    private int requiredQuantity;

    @Builder
    public Bom(Item finishedItem, Item rawMaterial, int requiredQuantity) {
        validateQuantity(requiredQuantity);
        this.finishedItem = finishedItem;
        this.rawMaterial = rawMaterial;
        this.requiredQuantity = requiredQuantity;
    }

    public void updateRequiredQuantity(int requiredQuantity) {
        validateQuantity(requiredQuantity);
        this.requiredQuantity = requiredQuantity;
    }

    private void validateQuantity(int requiredQuantity) {
        if (requiredQuantity <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "소요 수량은 1 이상이어야 합니다.");
        }
    }
}
