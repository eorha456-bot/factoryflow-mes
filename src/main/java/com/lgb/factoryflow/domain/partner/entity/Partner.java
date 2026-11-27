package com.lgb.factoryflow.domain.partner.entity;

import com.lgb.factoryflow.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "partners")
public class Partner extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String partnerCode;

    @Column(nullable = false, length = 100)
    private String partnerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PartnerType partnerType;

    @Column(length = 30)
    private String businessNumber;

    @Column(length = 30)
    private String phone;

    @Column(length = 200)
    private String address;

    @Column(nullable = false)
    private boolean active;

    @Builder
    public Partner(
            String partnerCode,
            String partnerName,
            PartnerType partnerType,
            String businessNumber,
            String phone,
            String address,
            Boolean active
    ) {
        this.partnerCode = partnerCode;
        this.partnerName = partnerName;
        this.partnerType = partnerType;
        this.businessNumber = businessNumber;
        this.phone = phone;
        this.address = address;
        this.active = active == null || active;
    }

    public void update(
            String partnerName,
            PartnerType partnerType,
            String businessNumber,
            String phone,
            String address
    ) {
        this.partnerName = partnerName;
        this.partnerType = partnerType;
        this.businessNumber = businessNumber;
        this.phone = phone;
        this.address = address;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }
}
