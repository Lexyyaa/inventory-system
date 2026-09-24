package com.deepfine.inventorysystem.domain.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;

/**
 * 생성/변경 시각 매핑 <br>
 * - 값은 DB가 채우고 엔티티는 읽기만 한다 <br>
 * - 저장이 native 쿼리라 JPA Auditing은 쓰지 않는다 <br>
 */
@Getter
@MappedSuperclass
public abstract class BaseTimeEntity {

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;
}
