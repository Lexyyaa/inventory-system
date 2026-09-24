package com.deepfine.inventorysystem.support.transaction;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 작업을 항상 새 트랜잭션(REQUIRES_NEW)에서 실행한다.
 *
 * <p>이 과제의 입출고 흐름에는 쓰지 않는다.
 * 입고 · 출고는 ApplicationService의 트랜잭션 하나 안에서 원자 SQL로 끝나고,
 * REQUIRES_NEW로 나누면 상품만 생성되고 재고는 반영되지 않은 상태가 남을 수 있다 (docs/design/03-domain-model.md §14).
 *
 * <p>용도:트랜잭션 밖에서 조회한 뒤, 락 조회가 첫 쿼리인 새 트랜잭션을 시작해야 할 때 쓴다.
 * REPEATABLE READ 스냅샷은 첫 일반 조회 시점에 고정되므로, 락보다 앞선 조회와 같은 트랜잭션에 있으면 안 된다.
 * 같은 클래스 안의 {@code @Transactional} 호출이 프록시를 타지 않는 문제도 이 빈으로 피한다.
 *
 * <p>전제: 호출자에 트랜잭션이 없어야 한다.
 * <ul>
 *   <li>호출자 트랜잭션 안에서 부르면 바깥 트랜잭션이 보류된 채 커넥션을 하나 더 잡는다.
 *       커넥션 풀이 작으면 동시 요청에서 고갈된다.
 *   <li>바깥 트랜잭션이 락을 쥔 채 부르고, 작업이 같은 행을 잠그면 자기 자신을 기다리다 락 타임아웃이 난다.
 * </ul>
 */
@Component
public class TransactionRunner {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> T requiresNew(Supplier<T> work) {
        return work.get();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void requiresNew(Runnable work) {
        work.run();
    }
}
