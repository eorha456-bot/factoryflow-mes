package com.lgb.factoryflow.domain.production.entity;

import com.lgb.factoryflow.domain.item.entity.Item;
import com.lgb.factoryflow.domain.item.entity.ItemType;
import com.lgb.factoryflow.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkOrderTest {

    private WorkOrder createSampleOrder() {
        Item box = Item.builder()
                .itemCode("BOX-TEST")
                .itemName("테스트 박스")
                .itemType(ItemType.FINISHED_GOOD)
                .unit("개")
                .safetyStock(10)
                .active(true)
                .build();

        return WorkOrder.builder()
                .orderNumber("WO-TEST-001")
                .item(box)
                .targetQuantity(100)
                .plannedDate(LocalDate.of(2026, 11, 27))
                .build();
    }

    @Test
    @DisplayName("작업 중인 작업지시는 사유와 함께 일시정지하고 다시 재개할 수 있다")
    void pauseAndResumeWorkOrder() {
        WorkOrder order = createSampleOrder();
        order.changeStatus(WorkOrderStatus.IN_PROGRESS);

        order.changeStatus(WorkOrderStatus.PAUSED, "원자재 부족");
        assertThat(order.getStatus()).isEqualTo(WorkOrderStatus.PAUSED);
        assertThat(order.getPauseReason()).isEqualTo("원자재 부족");
        assertThat(order.getPausedAt()).isNotNull();

        order.changeStatus(WorkOrderStatus.IN_PROGRESS);
        assertThat(order.getStatus()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
        assertThat(order.getPauseReason()).isNull();
        assertThat(order.getPausedAt()).isNull();
    }

    @Test
    @DisplayName("대기 상태 작업지시를 바로 일시정지하거나 사유 없이 일시정지하면 예외가 발생한다")
    void pauseValidationErrors() {
        WorkOrder waitingOrder = createSampleOrder();
        assertThatThrownBy(() -> waitingOrder.changeStatus(WorkOrderStatus.PAUSED, "설비 점검"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("작업 중인 작업지시만 일시정지");

        waitingOrder.changeStatus(WorkOrderStatus.IN_PROGRESS);
        assertThatThrownBy(() -> waitingOrder.changeStatus(WorkOrderStatus.PAUSED, "   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("일시정지 사유는 필수");
    }

    @Test
    @DisplayName("일시정지된 작업지시에는 생산실적을 등록할 수 없고 재개 후에만 등록 가능하다")
    void blockProductionResultWhilePaused() {
        WorkOrder order = createSampleOrder();
        order.changeStatus(WorkOrderStatus.IN_PROGRESS);
        order.changeStatus(WorkOrderStatus.PAUSED, "품질 이상 확인");

        assertThatThrownBy(() -> order.recordProduction(10, 0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("일시정지된 작업지시에는 생산실적을 등록할 수 없습니다");

        order.changeStatus(WorkOrderStatus.IN_PROGRESS);
        order.recordProduction(10, 1);
        assertThat(order.getProducedGoodQuantity()).isEqualTo(10);
        assertThat(order.getProducedDefectQuantity()).isEqualTo(1);
    }
}
