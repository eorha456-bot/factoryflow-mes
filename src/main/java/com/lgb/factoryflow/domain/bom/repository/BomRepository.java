package com.lgb.factoryflow.domain.bom.repository;

import com.lgb.factoryflow.domain.bom.entity.Bom;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BomRepository extends JpaRepository<Bom, Long> {

    boolean existsByFinishedItemIdAndRawMaterialId(Long finishedItemId, Long rawMaterialId);

    @EntityGraph(attributePaths = {"finishedItem", "rawMaterial"})
    List<Bom> findAllByOrderByFinishedItemIdAscIdAsc();

    @EntityGraph(attributePaths = {"finishedItem", "rawMaterial"})
    List<Bom> findByFinishedItemIdOrderByIdAsc(Long finishedItemId);

    @EntityGraph(attributePaths = {"finishedItem", "rawMaterial"})
    Optional<Bom> findWithItemsById(Long id);
}
