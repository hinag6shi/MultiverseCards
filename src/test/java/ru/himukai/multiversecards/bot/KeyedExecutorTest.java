package ru.himukai.multiversecards.bot;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyedExecutorTest {

    @Test
    void tasksOfOneKeyRunInOrderAndNeverOverlap() {
        List<Integer> order = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger running = new AtomicInteger();
        AtomicInteger maxRunning = new AtomicInteger();

        try (KeyedExecutor executor = new KeyedExecutor(4)) {
            for (int i = 0; i < 100; i++) {
                int n = i;
                executor.submit(42, () -> {
                    maxRunning.accumulateAndGet(running.incrementAndGet(), Math::max);
                    order.add(n);
                    running.decrementAndGet();
                });
            }
        }

        assertEquals(100, order.size());
        for (int i = 0; i < 100; i++) {
            assertEquals(i, order.get(i).intValue());
        }
        assertEquals(1, maxRunning.get());
    }

    @Test
    void differentKeysRunInParallel() throws InterruptedException {
        // ключи 0 и 1 при двух полосах попадают в разные полосы
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch firstSawSecond = new CountDownLatch(1);

        try (KeyedExecutor executor = new KeyedExecutor(2)) {
            executor.submit(0, () -> {
                try {
                    if (secondStarted.await(5, TimeUnit.SECONDS)) {
                        firstSawSecond.countDown();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            executor.submit(1, secondStarted::countDown);

            assertTrue(firstSawSecond.await(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void failingTaskDoesNotStopTheLane() {
        List<String> done = Collections.synchronizedList(new ArrayList<>());

        try (KeyedExecutor executor = new KeyedExecutor(1)) {
            executor.submit(1, () -> {
                throw new IllegalStateException("boom");
            });
            executor.submit(1, () -> done.add("after"));
        }

        assertEquals(List.of("after"), done);
    }

    @Test
    void closeWaitsForQueuedTasks() {
        AtomicInteger finished = new AtomicInteger();

        try (KeyedExecutor executor = new KeyedExecutor(1)) {
            for (int i = 0; i < 5; i++) {
                executor.submit(7, () -> {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    finished.incrementAndGet();
                });
            }
        }

        assertEquals(5, finished.get());
    }

    @Test
    void laneCountMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new KeyedExecutor(0));
    }
}
