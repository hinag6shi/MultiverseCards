package ru.himukai.multiversecards.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Несколько однопоточных «полос». Задачи с одним ключом (id игрока) всегда попадают в одну полосу
 * и выполняются строго по очереди, задачи с разными ключами могут выполняться параллельно.
 * Два разных игрока, попавшие в одну полосу, просто ждут друг друга.
 */
public final class KeyedExecutor implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(KeyedExecutor.class);
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 10;

    private final List<ExecutorService> lanes;

    public KeyedExecutor(int laneCount) {
        if (laneCount < 1) {
            throw new IllegalArgumentException("laneCount должно быть >= 1");
        }
        List<ExecutorService> created = new ArrayList<>();
        for (int i = 0; i < laneCount; i++) {
            String threadName = "user-lane-" + i;
            created.add(Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, threadName);
                thread.setDaemon(true);
                return thread;
            }));
        }
        this.lanes = List.copyOf(created);
    }

    /** Ставит задачу в очередь полосы этого ключа и сразу возвращает управление. */
    public void submit(long key, Runnable task) {
        Objects.requireNonNull(task, "task");
        ExecutorService lane = lanes.get(Math.floorMod(Long.hashCode(key), lanes.size()));
        try {
            lane.execute(() -> run(key, task));
        } catch (RejectedExecutionException e) {
            log.warn("Обновление пользователя {} отброшено: обработчик остановлен", key);
        }
    }

    /** Ошибка в одной задаче не должна ронять полосу и обработку остальных игроков. */
    private static void run(long key, Runnable task) {
        try {
            task.run();
        } catch (RuntimeException e) {
            log.error("Ошибка при обработке обновления пользователя {}", key, e);
        }
    }

    /** Дожидается уже принятых задач (до 10 секунд), новые не принимает. */
    @Override
    public void close() {
        lanes.forEach(ExecutorService::shutdown);
        try {
            for (ExecutorService lane : lanes) {
                if (!lane.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    lane.shutdownNow();
                }
            }
        } catch (InterruptedException e) {
            lanes.forEach(ExecutorService::shutdownNow);
            Thread.currentThread().interrupt();
        }
    }
}
