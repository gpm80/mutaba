package ru.micode.limiter;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тестирование ограничителя.
 */
class TaskLimiterTest {

    /**
     * Тестирование ограничителя в однопоточном режиме.
     */
    @Test
    public void testSinglePriorityBalancer() {
        TaskLimiter<String, Integer> taskPriorityBalancer = new TaskLimiter<>(1, "Single", 10, 50, 5);
        Optional<Integer> result = taskPriorityBalancer.createTask("task", 1, req -> {
            if (req != null) {
                return req.length();
            }
            return -1;
        }).waitFor(1000, TimeUnit.MILLISECONDS);
        Integer intValue = result.orElseThrow(RuntimeException::new);
        assertEquals(4, (int) intValue);
    }

    /**
     * Тестирование ограничителя в многопоточном режиме.
     *
     * @throws InterruptedException если что-то пошло не так.
     */
    @Test
    public void testMultiPriorityBalancer() throws InterruptedException {
        TaskLimiter<String, Integer> taskBalancer = new TaskLimiter<>(10, "Multi", 10, 100, 5);
        int count = 100;
        ExecutorService executor = Executors.newFixedThreadPool(count);
        CountDownLatch counter = new CountDownLatch(count);
        while (count-- > 0) {
            executor.submit(() -> {
                Random random = new Random();
                String s = StringUtils.repeat("y", random.nextInt(10) + 1);
                TaskWrapper<String, Integer> task = taskBalancer.createTask(s, 5, (val) -> {
                    TimeUnit.MILLISECONDS.sleep(random.nextInt(10));
                    return val.length();
                });
                try {
                    Integer integer = task.waitForThrow(30, TimeUnit.SECONDS);
                    System.out.printf("success value: (%s)%n", integer);
                    assertEquals((int) integer, s.length(), "don't equals result");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                counter.countDown();
            });
        }
        counter.await();
    }

    /**
     * Тестирование счетчиков, используемых в ограничителе.
     *
     * @throws InterruptedException если что-то пошло не так.
     */
    @Test
    public void testTimeLimitCounters() throws InterruptedException {
        Random rnd = new Random();
        TimeLimitCounter secondLimitCounter = new TimeLimitCounter(60, 1, TimeUnit.SECONDS);
        TimeLimitCounter minuteLimitCounter = new TimeLimitCounter(0, 1, TimeUnit.MINUTES);
        int threads = 10;
        CountDownLatch countDownLatch = new CountDownLatch(10 * threads);
        ExecutorService exec = Executors.newFixedThreadPool(threads);
        while (threads-- > 0) {
            exec.submit(() -> {
                long count = 0;
                while ((count = countDownLatch.getCount()) > 0) {
                    System.out.printf("(%s)\tminute: '%s'\tsec: '%s'%n", count,
                        minuteLimitCounter.incrementAndGet(), secondLimitCounter.decrementAndGet());
                    countDownLatch.countDown();
                    try {
                        TimeUnit.MILLISECONDS.sleep(rnd.nextInt(50) + 100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        countDownLatch.await();
    }
}