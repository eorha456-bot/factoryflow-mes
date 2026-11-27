package com.lgb.factoryflow.domain.item.service;

import com.lgb.factoryflow.domain.item.dto.ItemCreateRequest;
import com.lgb.factoryflow.domain.item.dto.ItemResponse;
import com.lgb.factoryflow.domain.item.dto.ItemStatusUpdateRequest;
import com.lgb.factoryflow.domain.item.dto.ItemUpdateRequest;
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
public class ItemService {

    private final ItemRepository itemRepository;

    @Transactional
    public ItemResponse createItem(ItemCreateRequest request) {
        String normalizedCode = request.getItemCode().trim().toUpperCase();
        if (itemRepository.existsByItemCode(normalizedCode)) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 등록된 품목 코드입니다: " + normalizedCode);
        }

        Item savedItem = itemRepository.save(request.toEntity(normalizedCode));
        return ItemResponse.from(savedItem);
    }

    public List<ItemResponse> getItems(ItemType itemType, Boolean active) {
        List<Item> items;
        if (itemType != null && active != null) {
            items = itemRepository.findByItemTypeAndActiveOrderByIdDesc(itemType, active);
        } else if (itemType != null) {
            items = itemRepository.findByItemTypeOrderByIdDesc(itemType);
        } else if (active != null) {
            items = itemRepository.findByActiveOrderByIdDesc(active);
        } else {
            items = itemRepository.findAllByOrderByIdDesc();
        }
        return items.stream()
                .map(ItemResponse::from)
                .toList();
    }

    public ItemResponse getItem(Long id) {
        Item item = findItemById(id);
        return ItemResponse.from(item);
    }

    @Transactional
    public ItemResponse updateItem(Long id, ItemUpdateRequest request) {
        Item item = findItemById(id);
        String normalizedSpec = request.getSpec() == null || request.getSpec().isBlank()
                ? null
                : request.getSpec().trim();

        item.update(
                request.getItemName().trim(),
                request.getItemType(),
                normalizedSpec,
                request.getUnit().trim(),
                request.getSafetyStock()
        );
        return ItemResponse.from(item);
    }

    @Transactional
    public ItemResponse updateItemStatus(Long id, ItemStatusUpdateRequest request) {
        Item item = findItemById(id);
        item.changeActive(request.getActive());
        return ItemResponse.from(item);
    }

    private Item findItemById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "품목을 찾을 수 없습니다. id=" + id));
    }
}
