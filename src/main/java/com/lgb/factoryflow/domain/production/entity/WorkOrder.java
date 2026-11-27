package com.lgb.factoryflow.domain.production.entity;

import com.lgb.factoryflow.domain.item.entity.Item;
import com.lgb.factoryflow.global.entity.BaseTimeEntity;
import com.lgb.factoryflow.global.exception.BusinessException;
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
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "work_orders")
public class WorkOrder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private int targetQuantity;

    @Column(nullable = false)
    private int producedGoodQuantity;

    @Column(nullable = false)
    private int producedDefectQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkOrderStatus status;

    @Column(nullable = false)
    private LocalDate plannedDate;

    @Column(length = 255)
    private String remark;

    @Version
    private Long version;

    @Builder
    public WorkOrder(
            String orderNumber,
            Item item,
            int targetQuantity,
            LocalDate plannedDate,
            String remark
    ) {
        if (targetQuantity <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "목표 생산 수량은 1 이상이어야 합니다.");
        }
        this.orderNumber = orderNumber;
        this.item = item;
        this.targetQuantity = targetQuantity;
        this.producedGoodQuantity = 0;
        this.producedDefectQuantity = 0;
        this.status = WorkOrderStatus.WAITING;
        this.plannedDate = plannedDate;
        this.remark = remark;
    }

    public void changeStatus(WorkOrderStatus nextStatus) {
        if (this.status == nextStatus) {
            return;
        }
        if (this.status == WorkOrderStatus.COMPLETED || this.status == WorkOrderStatus.CANCELED) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "이미 " + this.status.getDescription() + "된 작업지시는 상태를 변경할 수 없습니다."
            );
        }
        if (nextStatus == WorkOrderStatus.WAITING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "작업 중 상태에서 대기 상태로 되돌릴 수 없습니다.");
        }
        if (nextStatus == WorkOrderStatus.CANCELED
                && (this.producedGoodQuantity > 0 || this.producedDefectQuantity > 0)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "이미 생산실적이 등록된 작업지시는 취소할 수 없습니다.");
        }
        this.status = nextStatus;
    }

    public void recordProduction(int goodQuantity, int defectQuantity) {
        if (this.status == WorkOrderStatus.COMPLETED || this.status == WorkOrderStatus.CANCELED) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    this.status.getDescription() + " 상태의 작업지시에는 생산실적을 등록할 수 없습니다."
            );
        }
        if (goodQuantity < 0 || defectQuantity < 0 || (goodQuantity + defectQuantity) <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "양품 수량과 불량 수량의 합은 1 이상이어야 합니다.");
        }

        if (this.status == WorkOrderStatus.WAITING) {
            this.status = WorkOrderStatus.IN_PROGRESS;
        }

        this.producedGoodQuantity += goodQuantity;
        this.producedDefectQuantity += defectQuantity;

        if (this.producedGoodQuantity >= this.targetQuantity) {
            this.status = WorkOrderStatus.COMPLETED;
        }
    }

    public double calculateProgressRate() {
        if (this.targetQuantity <= 0) {
            return 0.0;
        }
        double rate = (this.producedGoodQuantity * 100.0) / this.targetQuantity;
        return Math.round(rate * 10.0) / 10.0;
    }
}
