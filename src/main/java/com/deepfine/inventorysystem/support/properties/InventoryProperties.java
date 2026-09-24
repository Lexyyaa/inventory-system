package com.deepfine.inventorysystem.support.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 재고 정책 수치 ({@code application.yml}의 {@code inventory.*}).
 *
 * @param maxQuantity 입고 · 출고 요청 한 건의 수량 상한 (02 §3 "수량")
 */
@ConfigurationProperties(prefix = "inventory")
public record InventoryProperties(long maxQuantity) {}
