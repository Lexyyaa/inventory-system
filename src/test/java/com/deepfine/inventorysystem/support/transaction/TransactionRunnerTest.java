package com.deepfine.inventorysystem.support.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.deepfine.inventorysystem.support.IntegrationTest;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class TransactionRunnerTest {

    private static final String OUTER = "outer-tx";

    @Autowired
    TransactionRunner transactionRunner;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("트랜잭션이 없는 곳에서 부르면 새 트랜잭션 안에서 실행하고 값을 돌려준다")
    void runsInNewTransaction() {
        // given
        assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                .isFalse();

        // when
        Boolean active = transactionRunner.requiresNew(TransactionSynchronizationManager::isActualTransactionActive);

        // then
        assertThat(active).isTrue();
        assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                .isFalse();
    }

    @Test
    @DisplayName("바깥 트랜잭션 안에서 불러도 합류하지 않고 별도 트랜잭션에서 실행한다")
    void doesNotJoinOuterTransaction() {
        // given
        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        outer.setName(OUTER);
        AtomicReference<String> innerName = new AtomicReference<>();
        AtomicBoolean innerActive = new AtomicBoolean();
        AtomicReference<String> outerNameAfter = new AtomicReference<>();

        // when
        outer.executeWithoutResult(status -> {
            transactionRunner.requiresNew(() -> {
                innerName.set(TransactionSynchronizationManager.getCurrentTransactionName());
                innerActive.set(TransactionSynchronizationManager.isActualTransactionActive());
            });
            outerNameAfter.set(TransactionSynchronizationManager.getCurrentTransactionName());
        });

        // then
        assertThat(innerActive).isTrue();
        assertThat(innerName.get()).isNotNull().isNotEqualTo(OUTER);
        assertThat(outerNameAfter.get()).isEqualTo(OUTER);
    }
}
