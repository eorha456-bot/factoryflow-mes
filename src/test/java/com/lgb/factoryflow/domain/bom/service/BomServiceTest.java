package com.lgb.factoryflow.domain.bom.service;

import com.lgb.factoryflow.domain.bom.dto.BomCreateRequest;
import com.lgb.factoryflow.domain.bom.dto.BomResponse;
import com.lgb.factoryflow.domain.bom.dto.BomUpdateRequest;
import com.lgb.factoryflow.domain.bom.entity.Bom;
import com.lgb.factoryflow.domain.bom.repository.BomRepository;
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
class BomServiceTest {

    @Mock
    private BomRepository bomRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private BomService bomService;

    @Test
    @DisplayName("완제품과 원재료로 BOM을 정상 등록하고 소요 수량을 수정한다")
    void createAndUpdateBomSuccess() {
        Item box = createItem(1L, "BOX-003", "택배박스 3호", ItemType.FINISHED_GOOD);
        Item sheet = createItem(2L, "RAW-AB-01", "AB골 원단", ItemType.RAW_MATERIAL);

        given(itemRepository.findById(1L)).willReturn(Optional.of(box));
        given(itemRepository.findById(2L)).willReturn(Optional.of(sheet));
        given(bomRepository.existsByFinishedItemIdAndRawMaterialId(1L, 2L)).willReturn(false);
        given(bomRepository.save(any(Bom.class))).willAnswer(invocation -> invocation.getArgument(0));

        BomResponse created = bomService.createBom(new BomCreateRequest(1L, 2L, 2));

        assertThat(created.getFinishedItemCode()).isEqualTo("BOX-003");
        assertThat(created.getRawMaterialCode()).isEqualTo("RAW-AB-01");
        assertThat(created.getRequiredQuantity()).isEqualTo(2);

        Bom bom = Bom.builder().finishedItem(box).rawMaterial(sheet).requiredQuantity(2).build();
        given(bomRepository.findWithItemsById(10L)).willReturn(Optional.of(bom));

        BomResponse updated = bomService.updateBom(10L, new BomUpdateRequest(3));
        assertThat(updated.getRequiredQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("완제품 자리에 원재료를 넣으면 400 BAD_REQUEST 예외가 발생한다")
    void createBomWrongItemTypeThrowsBadRequest() {
        Item raw1 = createItem(1L, "RAW-01", "A골 원단", ItemType.RAW_MATERIAL);
        Item raw2 = createItem(2L, "RAW-02", "접착제", ItemType.RAW_MATERIAL);

        given(itemRepository.findById(1L)).willReturn(Optional.of(raw1));
        given(itemRepository.findById(2L)).willReturn(Optional.of(raw2));

        assertThatThrownBy(() -> bomService.createBom(new BomCreateRequest(1L, 2L, 1)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(bomRepository, never()).save(any(Bom.class));
    }

    @Test
    @DisplayName("중복된 완제품-원재료 조합 등록 시 409 CONFLICT 예외가 발생한다")
    void createBomDuplicateThrowsConflict() {
        Item box = createItem(1L, "BOX-003", "택배박스 3호", ItemType.FINISHED_GOOD);
        Item sheet = createItem(2L, "RAW-AB-01", "AB골 원단", ItemType.RAW_MATERIAL);

        given(itemRepository.findById(1L)).willReturn(Optional.of(box));
        given(itemRepository.findById(2L)).willReturn(Optional.of(sheet));
        given(bomRepository.existsByFinishedItemIdAndRawMaterialId(1L, 2L)).willReturn(true);

        assertThatThrownBy(() -> bomService.createBom(new BomCreateRequest(1L, 2L, 1)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    private Item createItem(Long id, String code, String name, ItemType type) {
        Item item = Item.builder()
                .itemCode(code)
                .itemName(name)
                .itemType(type)
                .unit("개")
                .safetyStock(10)
                .active(true)
                .build();
        try {
            var field = Item.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(item, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return item;
    }
}
