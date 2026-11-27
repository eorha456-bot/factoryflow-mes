package com.lgb.factoryflow.domain.production.entity;

import com.lgb.factoryflow.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "production_results")
public class ProductionResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @Column(nullable = false)
    private int goodQuantity;

    @Column(nullable = false)
    private int defectQuantity;

    @Column(length = 50)
    private String workerName;

    @Column(length = 255)
    private String remark;

    @Builder
    public ProductionResult(
            WorkOrder workOrder,
            int goodQuantity,
            int defectQuantity,
            String workerName,
            String remark
    ) {
        this.workOrder = workOrder;
        this.goodQuantity = goodQuantity;
        this.defectQuantity = defectQuantity;
        this.workerName = workerName;
        this.remark = remark;
    }

    public int getTotalQuantity() {
        return this.goodQuantity + this.defectQuantity;
    }
}
