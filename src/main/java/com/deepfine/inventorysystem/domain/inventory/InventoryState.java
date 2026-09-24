package com.deepfine.inventorysystem.domain.inventory;

import java.time.Instant;

public record InventoryState(Long quantity, Instant updatedAt) {}
