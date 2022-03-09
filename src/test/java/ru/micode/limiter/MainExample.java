package ru.micode.limiter;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Пример использования ограничителя.
 */
public class MainExample {

    public static void main(String[] args) throws InterruptedException {
        // Пулл потоков
        final ExecutorService pool = Executors.newCachedThreadPool();
        // Счетчик успешно выполненных задач.
        AtomicInteger success = new AtomicInteger();
        final Random random = new Random();
        // Инициализачия ограничителя (3 операции в секунду, и 100 операций в минуту.)
        // Приоритет сохраняемой операции 3 (приортиетты меньше 3 не гарантируют выполнение задачи)
        final TaskLimiter<String, Integer> taskLimiter = new TaskLimiter<>(3, "cals-len-string", 3, 100, 3);
        String[] tasks = new String[] {"aa", "bbbbbb", "ccccccc", "eeeeee", "dddd", "f", "tt", "qqqq", "www"};
        // Создаем поток задач
        CountDownLatch latch = new CountDownLatch(100);
        for (int i = 0; i < 100; i++) {
            pool.execute(() -> {
                final String str = tasks[random.nextInt(tasks.length)];
                final int priority = random.nextInt(5);
                final Optional<Integer> result = taskLimiter.createTask(str, priority, val -> {
                    TimeUnit.MILLISECONDS.sleep(random.nextInt(50));
                    return val.length();
                }).waitFor(20, TimeUnit.SECONDS);
                if (result.isPresent()) {
                    success.incrementAndGet();
                    System.out.printf("len: %s = %s %n", str, result.get());
                } else {
                    System.err.printf("len: %s = ignore priority: %s %n", str, priority);
                }
                latch.countDown();
            });
        }
        // Ожидаем обработки всех заданий
        latch.await();
        pool.shutdown();
        System.out.printf("%s/100%n", success.get());
    }
}
