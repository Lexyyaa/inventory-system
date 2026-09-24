package com.deepfine.inventorysystem.domain.inventory;

import java.time.OffsetDateTime;

/**
 * 재고 변경 원자 SQL의 {@code RETURNING quantity, updated_at} 결과. 이 요청이 반영된 직후의 재고다.
 * 응답의 수량 · 변경 시각은 이 값만 쓴다 (원자 SQL 전에 읽은 값은 쓰지 않는다).
 */
public record InventorySnapshot(Long quantity, OffsetDateTime updatedAt) {}
