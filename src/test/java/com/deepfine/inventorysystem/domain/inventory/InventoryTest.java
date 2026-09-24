package com.deepfine.inventorysystem.domain.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import com.deepfine.inventorysystem.domain.exception.ErrorCode;
import com.deepfine.inventorysystem.domain.inventory.exception.InventoryException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InventoryTest {

    private static final long MAX_QUANTITY = 1_000_000_000L;

    @Test
    @DisplayName("[TC-2-14] 입고 수량이 1 이상 상한 이하일 때만 통과하고 벗어나면 INVALID_QUANTITY로 거부한다")
    void validatesInboundQuantityRange() {
        // given
        long max = MAX_QUANTITY;

        // when
        Throwable negative = catchThrowable(() -> Inventory.validateInboundQuantity(-1, max));
        Throwable zero = catchThrowable(() -> Inventory.validateInboundQuantity(0, max));
        Throwable min = catchThrowable(() -> Inventory.validateInboundQuantity(1, max));
        Throwable atMax = catchThrowable(() -> Inventory.validateInboundQuantity(max, max));
        Throwable overMax = catchThrowable(() -> Inventory.validateInboundQuantity(max + 1, max));

        // then
        assertThat(min).isNull();
        assertThat(atMax).isNull();
        assertInvalidQuantity(negative);
        assertInvalidQuantity(zero);
        assertInvalidQuantity(overMax);
    }

    private static void assertInvalidQuantity(Throwable thrown) {
        assertThat(thrown)
                .asInstanceOf(type(InventoryException.class))
                .extracting(InventoryException::getErrorCode)
                .isEqualTo(ErrorCode.INVALID_QUANTITY);
    }
}
