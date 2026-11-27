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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BomService {

    private final BomRepository bomRepository;
    private final ItemRepository itemRepository;

    @Transactional
    public BomResponse createBom(BomCreateRequest request) {
        Item finishedItem = findItemById(request.getFinishedItemId(), "완제품");
        Item rawMaterial = findItemById(request.getRawMaterialId(), "원재료");

        if (finishedItem.getItemType() != ItemType.FINISHED_GOOD) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "BOM의 완제품 품목은 FINISHED_GOOD 구분이어야 합니다.");
        }
        if (rawMaterial.getItemType() != ItemType.RAW_MATERIAL) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "BOM의 원재료 품목은 RAW_MATERIAL 구분이어야 합니다.");
        }
        if (!finishedItem.isActive() || !rawMaterial.isActive()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "비활성화된 품목으로는 BOM을 등록할 수 없습니다.");
        }
        if (bomRepository.existsByFinishedItemIdAndRawMaterialId(finishedItem.getId(), rawMaterial.getId())) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 등록된 완제품-원재료 BOM 조합입니다.");
        }

        Bom saved = bomRepository.save(Bom.builder()
                .finishedItem(finishedItem)
                .rawMaterial(rawMaterial)
                .requiredQuantity(request.getRequiredQuantity())
                .build());
        return BomResponse.from(saved);
    }

    public List<BomResponse> getBoms(Long finishedItemId) {
        List<Bom> boms = finishedItemId != null
                ? bomRepository.findByFinishedItemIdOrderByIdAsc(finishedItemId)
                : bomRepository.findAllByOrderByFinishedItemIdAscIdAsc();
        return boms.stream()
                .map(BomResponse::from)
                .toList();
    }

    @Transactional
    public BomResponse updateBom(Long id, BomUpdateRequest request) {
        Bom bom = bomRepository.findWithItemsById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "BOM 정보를 찾을 수 없습니다. id=" + id));
        bom.updateRequiredQuantity(request.getRequiredQuantity());
        return BomResponse.from(bom);
    }

    @Transactional
    public void deleteBom(Long id) {
        Bom bom = bomRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "BOM 정보를 찾을 수 없습니다. id=" + id));
        bomRepository.delete(bom);
    }

    private Item findItemById(Long itemId, String label) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, label + " 품목을 찾을 수 없습니다. id=" + itemId));
    }
}
