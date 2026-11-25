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
 * Несколько очередей, каждая со своим потоком
 * Команды одного игрока всегда идут в одну очередь и не обгоняют друг друга
 * Разные игроки могут обрабатываться параллельно, а если попали в одну очередь — чуть подождут
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

    /** Кладёт задачу в очередь игрока и сразу возвращается */
    public void submit(long key, Runnable task) {
        Objects.requireNonNull(task, "task");
        ExecutorService lane = lanes.get(Math.floorMod(Long.hashCode(key), lanes.size()));
        try {
            lane.execute(() -> run(key, task));
        } catch (RejectedExecutionException e) {
            log.warn("Обновление пользователя {} отброшено: обработчик остановлен", key);
        }
    }

    /** Если одна команда упала, очередь всё равно продолжит разбирать остальные */
    private static void run(long key, Runnable task) {
        try {
            task.run();
        } catch (RuntimeException e) {
            log.error("Ошибка при обработке обновления пользователя {}", key, e);
        }
    }

    /** Перестаёт принимать задачи и ждёт старые до 10 секунд */
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
