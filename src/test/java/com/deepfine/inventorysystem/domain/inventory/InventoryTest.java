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
    private static final String OUTBOUND_QUANTITY_MESSAGE = "출고 수량이 허용 범위를 벗어났습니다.";

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

    @Test
    @DisplayName("[TC-4-10] 출고 수량이 1 이상 상한 이하일 때만 통과하고 벗어나면 출고 문구의 INVALID_QUANTITY로 거부한다")
    void validatesOutboundQuantityRange() {
        // given
        long max = MAX_QUANTITY;

        // when
        Throwable negative = catchThrowable(() -> Inventory.validateOutboundQuantity(-1, max));
        Throwable zero = catchThrowable(() -> Inventory.validateOutboundQuantity(0, max));
        Throwable min = catchThrowable(() -> Inventory.validateOutboundQuantity(1, max));
        Throwable atMax = catchThrowable(() -> Inventory.validateOutboundQuantity(max, max));
        Throwable overMax = catchThrowable(() -> Inventory.validateOutboundQuantity(max + 1, max));

        // then
        assertThat(min).isNull();
        assertThat(atMax).isNull();
        assertInvalidOutboundQuantity(negative);
        assertInvalidOutboundQuantity(zero);
        assertInvalidOutboundQuantity(overMax);
    }

    private static void assertInvalidOutboundQuantity(Throwable thrown) {
        assertInvalidQuantity(thrown);
        assertThat(thrown).hasMessage(OUTBOUND_QUANTITY_MESSAGE);
    }

    private static void assertInvalidQuantity(Throwable thrown) {
        assertThat(thrown)
                .asInstanceOf(type(InventoryException.class))
                .extracting(InventoryException::getErrorCode)
                .isEqualTo(ErrorCode.INVALID_QUANTITY);
    }
}
