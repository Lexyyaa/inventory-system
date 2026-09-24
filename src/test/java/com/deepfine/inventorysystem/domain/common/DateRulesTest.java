package com.deepfine.inventorysystem.domain.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DateRulesTest {

    @ParameterizedTest
    @ValueSource(strings = {"1000-01-01", "9999-12-31", "2026-01-01"})
    @DisplayName("저장 가능 범위의 양끝과 안쪽 날짜는 저장할 수 있다")
    void storable(String date) {
        // given
        LocalDate value = LocalDate.parse(date);

        // when & then
        assertThat(DateRules.isStorable(value)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0999-12-31", "+10000-01-01", "-0001-01-01", "+999999999-12-31", "-999999999-01-01"})
    @DisplayName("저장 가능 범위 밖의 날짜는 저장할 수 없다")
    void notStorable(String date) {
        // given
        LocalDate value = LocalDate.parse(date);

        // when & then
        assertThat(DateRules.isStorable(value)).isFalse();
    }

    @Test
    @DisplayName("null은 저장할 수 없다")
    void nullNotStorable() {
        assertThat(DateRules.isStorable(null)).isFalse();
    }

    @Test
    @DisplayName("더한 결과가 상한과 같으면 허용하고 하루라도 넘으면 거부한다")
    void canAddDaysUpperBound() {
        // given
        LocalDate base = LocalDate.of(9999, 12, 29);

        // when & then
        assertThat(DateRules.canAddDays(base, 2)).isTrue();
        assertThat(DateRules.canAddDays(base, 3)).isFalse();
    }

    @Test
    @DisplayName("음수를 더한 결과가 하한과 같으면 허용하고 하루라도 넘으면 거부한다")
    void canAddDaysLowerBound() {
        // given
        LocalDate base = LocalDate.of(1000, 1, 3);

        // when & then
        assertThat(DateRules.canAddDays(base, -2)).isTrue();
        assertThat(DateRules.canAddDays(base, -3)).isFalse();
    }

    @Test
    @DisplayName("기준일이 범위 밖이거나 일수가 극단값이면 예외 없이 거부한다")
    void canAddDaysExtreme() {
        // given
        LocalDate base = LocalDate.of(2026, 1, 1);

        // when & then
        assertThat(DateRules.canAddDays(LocalDate.MAX, 0)).isFalse();
        assertThat(DateRules.canAddDays(null, 0)).isFalse();
        assertThat(DateRules.canAddDays(base, Long.MAX_VALUE)).isFalse();
        assertThat(DateRules.canAddDays(base, Long.MIN_VALUE)).isFalse();
    }
}
