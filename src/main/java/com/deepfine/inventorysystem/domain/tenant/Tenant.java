package com.deepfine.inventorysystem.domain.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품과 재고를 구분하는 업체 단위. 요청 헤더 X-Tenant-Id의 값이 {@code code}다.
 *
 * <p>업체는 기동 시 seed(data.sql)로만 등록한다. 생성 · 변경 API가 없으므로 생성 팩토리와 상태 변경 메서드를 두지 않는다.
 * {@code created_at}은 DB가 채운다.
 */
@Entity
@Table(name = "tenant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
