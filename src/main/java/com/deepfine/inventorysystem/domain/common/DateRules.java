package com.deepfine.inventorysystem.domain.common;

import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * DB에 저장할 수 있는 날짜 범위 판정. 판정만 하고, 예외는 호출하는 도메인이 자기 ErrorCode로 던진다.
 *
 * <p>날짜 계산({@code plusDays} 등) 전에 범위를 먼저 검사한다.
 * ISO 파싱은 {@code +999999999-12-31}까지 받아들이므로 검사 없이 계산하면 {@code DateTimeException}(500)이 난다.
 *
 * <p>PostgreSQL DATE는 범위(BC 4713 ~ AD 5874897)가 넓지만, 그 밖의 값은 저장 시점에 500으로 터지고
 * 0 이하 연도는 JDBC에서 BC 날짜로 바뀌어 한 해 어긋난다. 그래서 업무상 의미 있는 1000-01-01 ~ 9999-12-31로 좁히고
 * 상한과 하한을 짝으로 막는다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DateRules {

    public static final LocalDate MIN_DATE = LocalDate.of(1000, 1, 1);
    public static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    /** null이 아니고 MIN_DATE ~ MAX_DATE(양끝 포함) 안이면 true. */
    public static boolean isStorable(LocalDate date) {
        return date != null && !date.isBefore(MIN_DATE) && !date.isAfter(MAX_DATE);
    }

    /** base가 저장 가능하고, base + days(음수 허용)도 저장 가능 범위 안이면 true. 계산 전에 부른다. */
    public static boolean canAddDays(LocalDate base, long days) {
        if (!isStorable(base)) {
            return false;
        }
        long epochDay = base.toEpochDay();
        return days <= MAX_DATE.toEpochDay() - epochDay && days >= MIN_DATE.toEpochDay() - epochDay;
    }
}
