package com.deepfine.inventorysystem.support.web;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 페이지 파라미터 상한. 컨트롤러에서 이렇게 쓴다.
 *
 * <pre>{@code
 * @RequestParam(defaultValue = "0") @Min(0) @Max(PageLimits.MAX_PAGE) int page,
 * @RequestParam(defaultValue = "20") @Min(1) @Max(PageLimits.MAX_SIZE) int size
 * }</pre>
 *
 * <p>상한이 없으면 offset(page × size)이 int를 넘어 Spring Data가 예외를 던지고 500이 된다.
 * MAX_PAGE는 (MAX_PAGE + 1) × MAX_SIZE가 int 범위 안에 들도록 잡았다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PageLimits {

    public static final int MAX_SIZE = 100;
    public static final int MAX_PAGE = Integer.MAX_VALUE / MAX_SIZE - 1;
}
