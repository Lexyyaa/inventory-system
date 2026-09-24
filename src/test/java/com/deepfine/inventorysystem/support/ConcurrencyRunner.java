package com.deepfine.inventorysystem.support;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.stream.IntStream;

public final class ConcurrencyRunner {

    private static final long TIMEOUT_SECONDS = 30;

    private ConcurrencyRunner() {}

    @FunctionalInterface
    public interface Task<T> {
        T execute(int index) throws Exception;
    }

    public record Result<T>(T value, Throwable error) {
        public boolean isSuccess() {
            return error == null;
        }
    }

    public static <T> List<Result<T>> run(int threadCount, Task<T> task) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicReferenceArray<Result<T>> results = new AtomicReferenceArray<>(threadCount);

        for (int i = 0; i < threadCount; i++) {
            int index = i;
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    results.set(index, new Result<>(task.execute(index), null));
                } catch (Throwable t) {
                    results.set(index, new Result<>(null, t));
                } finally {
                    done.countDown();
                }
            });
        }

        boolean finished;
        try {
            ready.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            start.countDown();
            finished = done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
        if (!finished) {
            throw new IllegalStateException("동시성 작업이 " + TIMEOUT_SECONDS + "초 안에 끝나지 않았습니다 (데드락 의심)");
        }
        return IntStream.range(0, threadCount).mapToObj(results::get).toList();
    }

    public static <T> long successCount(List<Result<T>> results) {
        return results.stream().filter(Result::isSuccess).count();
    }

    public static <T> List<Throwable> errors(List<Result<T>> results) {
        return results.stream()
                .filter(result -> !result.isSuccess())
                .map(Result::error)
                .toList();
    }
}
