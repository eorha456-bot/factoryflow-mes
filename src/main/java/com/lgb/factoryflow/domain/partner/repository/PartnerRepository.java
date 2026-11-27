package com.lgb.factoryflow.domain.partner.repository;

import com.lgb.factoryflow.domain.partner.entity.Partner;
import com.lgb.factoryflow.domain.partner.entity.PartnerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerRepository extends JpaRepository<Partner, Long> {

    boolean existsByPartnerCode(String partnerCode);

    List<Partner> findAllByOrderByIdDesc();

    List<Partner> findByPartnerTypeOrderByIdDesc(PartnerType partnerType);

    List<Partner> findByActiveOrderByIdDesc(boolean active);

    List<Partner> findByPartnerTypeAndActiveOrderByIdDesc(PartnerType partnerType, boolean active);
}
