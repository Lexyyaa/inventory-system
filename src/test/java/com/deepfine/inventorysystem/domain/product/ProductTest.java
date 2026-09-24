package com.deepfine.inventorysystem.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import com.deepfine.inventorysystem.domain.exception.ErrorCode;
import com.deepfine.inventorysystem.domain.product.exception.ProductException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductTest {

    @Test
    @DisplayName("[TC-2-05] 상품명은 대소문자와 공백까지 같을 때만 같은 상품명으로 판정한다")
    void validatesExactProductName() {
        // given
        Product product = Product.create(1L, "A001", "Apple");

        // when
        Throwable exact = catchThrowable(() -> product.validateName("Apple"));
        Throwable lowerCase = catchThrowable(() -> product.validateName("apple"));
        Throwable trailingSpace = catchThrowable(() -> product.validateName("Apple "));
        Throwable leadingSpace = catchThrowable(() -> product.validateName(" Apple"));

        // then
        assertThat(exact).isNull();
        assertNameMismatch(lowerCase);
        assertNameMismatch(trailingSpace);
        assertNameMismatch(leadingSpace);
        assertThat(product.getName()).isEqualTo("Apple");
    }

    private static void assertNameMismatch(Throwable thrown) {
        assertThat(thrown)
                .asInstanceOf(type(ProductException.class))
                .extracting(ProductException::getErrorCode)
                .isEqualTo(ErrorCode.PRODUCT_NAME_MISMATCH);
    }
}
