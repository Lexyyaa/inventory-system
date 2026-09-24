package com.deepfine.inventorysystem.support.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PageLimitsTest {

    @Test
    @DisplayName("최대 페이지의 끝 offset이 int 범위를 넘지 않는다")
    void maxOffsetFitsInInt() {
        // given
        long lastOffsetEnd = (long) PageLimits.MAX_PAGE * PageLimits.MAX_SIZE + PageLimits.MAX_SIZE;

        // when & then
        assertThat(lastOffsetEnd).isLessThanOrEqualTo(Integer.MAX_VALUE);
        assertThat(PageLimits.MAX_PAGE).isPositive();
    }
}
