package com.deepfine.inventorysystem.support.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inventory")
public record InventoryProperties(long maxQuantity) {}
