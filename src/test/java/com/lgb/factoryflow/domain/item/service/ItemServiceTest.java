package com.lgb.factoryflow.domain.item.service;

import com.lgb.factoryflow.domain.item.dto.ItemCreateRequest;
import com.lgb.factoryflow.domain.item.dto.ItemResponse;
import com.lgb.factoryflow.domain.item.dto.ItemStatusUpdateRequest;
import com.lgb.factoryflow.domain.item.dto.ItemUpdateRequest;
import com.lgb.factoryflow.domain.item.entity.Item;
import com.lgb.factoryflow.domain.item.entity.ItemType;
import com.lgb.factoryflow.domain.item.repository.ItemRepository;
import com.lgb.factoryflow.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    @Test
    @DisplayName("품목 등록 시 코드는 대문자로 정규화되어 저장된다")
    void createItemSuccess() {
        ItemCreateRequest request = new ItemCreateRequest(
                " raw-ab-01 ",
                "AB골 원단",
                ItemType.RAW_MATERIAL,
                "1200x1000",
                "매",
                100
        );

        given(itemRepository.existsByItemCode("RAW-AB-01")).willReturn(false);
        given(itemRepository.save(any(Item.class))).willAnswer(invocation -> invocation.getArgument(0));

        ItemResponse response = itemService.createItem(request);

        assertThat(response.getItemCode()).isEqualTo("RAW-AB-01");
        assertThat(response.getItemName()).isEqualTo("AB골 원단");
        assertThat(response.getItemType()).isEqualTo(ItemType.RAW_MATERIAL);
        assertThat(response.getItemTypeDescription()).isEqualTo("원재료");
        assertThat(response.getSafetyStock()).isEqualTo(100);
        assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("이미 존재하는 품목 코드로 등록하면 409 CONFLICT 예외가 발생한다")
    void createItemDuplicateCodeThrowsConflict() {
        ItemCreateRequest request = new ItemCreateRequest(
                "BOX-003",
                "택배박스 3호",
                ItemType.FINISHED_GOOD,
                "300x200x150",
                "개",
                50
        );

        given(itemRepository.existsByItemCode("BOX-003")).willReturn(true);

        assertThatThrownBy(() -> itemService.createItem(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                });

        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    @DisplayName("품목 정보 수정 및 활성 상태 변경이 정상 반영된다")
    void updateItemAndStatusSuccess() {
        Item item = Item.builder()
                .itemCode("BOX-003")
                .itemName("택배박스 3호")
                .itemType(ItemType.FINISHED_GOOD)
                .spec("300x200x150")
                .unit("개")
                .safetyStock(50)
                .active(true)
                .build();

        given(itemRepository.findById(1L)).willReturn(Optional.of(item));

        ItemResponse updated = itemService.updateItem(1L, new ItemUpdateRequest(
                "택배박스 3호(강화)",
                ItemType.FINISHED_GOOD,
                "310x210x160",
                "개",
                80
        ));
        assertThat(updated.getItemName()).isEqualTo("택배박스 3호(강화)");
        assertThat(updated.getSafetyStock()).isEqualTo(80);

        ItemResponse deactivated = itemService.updateItemStatus(1L, new ItemStatusUpdateRequest(false));
        assertThat(deactivated.isActive()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 품목 ID 조회 시 404 NOT_FOUND 예외가 발생한다")
    void getItemNotFoundThrowsException() {
        given(itemRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.getItem(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }
}
