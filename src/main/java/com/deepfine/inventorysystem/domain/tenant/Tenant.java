package com.deepfine.inventorysystem.domain.tenant;

import com.deepfine.inventorysystem.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 업체 <br>
 * - 요청 헤더 X-Tenant-Id의 값이 code다 <br>
 * - seed(data.sql)로만 등록해서 생성 · 변경 메서드를 두지 않는다 <br>
 */
@Entity
@Table(name = "tenant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tenant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;
}
