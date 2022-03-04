package ru.micode.limiter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ограничитель выполнения задач, с приоритентой очередью.
 */
public class TaskLimiter<TASK, RESULT> {

    private static final Logger logger = LoggerFactory.getLogger(TaskLimiter.class);
    private final PriorityBlockingQueue<TaskWrapper<TASK, RESULT>> requestQueue;
    private final ExecutorService executorService;
    private final TimeLimitCounter secondCounter;
    private final TimeLimitCounter minuteCounter;
    private final AtomicBoolean work;

    /**
     * Конструктор.
     *
     * @param threadPoolSize   количество потоков обрабатывающих очередь запросов
     * @param nameBalancer     название балансера
     * @param requestPerSecond максимальное количество запросов в секунду
     * @param requestPerMinute максимальное количество запросов в минуту
     * @param safetyPriority   минимальное сохраняемое значение приоритета, при достижении любого из лимитов. Задачи с
     *                         более низким приоритетом не будут возвращаться в очередь для попыток выполнения запроса в
     *                         следующей итерации.
     */
    public TaskLimiter(int threadPoolSize, String nameBalancer, int requestPerSecond, int requestPerMinute,
                       int safetyPriority) {
        work = new AtomicBoolean(true);
        secondCounter = new TimeLimitCounter(requestPerSecond, 1, TimeUnit.SECONDS);
        minuteCounter = new TimeLimitCounter(requestPerMinute, 1, TimeUnit.MINUTES);
        if (nameBalancer != null) {
            secondCounter.setNameCounter(nameBalancer + "_" + TimeUnit.SECONDS.name());
            minuteCounter.setNameCounter(nameBalancer + "_" + TimeUnit.MINUTES.name());
        }
        requestQueue = new PriorityBlockingQueue<>();
        executorService = Executors.newFixedThreadPool(threadPoolSize, new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable);
                thread.setName("balancer-" + nameBalancer + "-" + counter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        });
        int i = threadPoolSize;
        while (i-- > 0) {
            executorService.submit(() -> {
                while (work.get()) {
                    try {
                        logger.trace("wait next task...");
                        TaskWrapper<TASK, RESULT> taskWrapper = requestQueue.take();
                        if (secondCounter.decrementAndGet() < 0) {
                            logger.debug("per second limit value = {}", secondCounter.get());
                            if (taskWrapper.getPriority() < safetyPriority) {
                                taskWrapper.exceptionally("Request per second limit exceeded");
                            } else {
                                // Вернем обратно в очередь
                                TimeUnit.MILLISECONDS.sleep(100);
                                requestQueue.offer(taskWrapper);
                            }
                        } else if (minuteCounter.decrementAndGet() < 0) {
                            logger.debug("per minute limit value = {}", minuteCounter.get());
                            if (taskWrapper.getPriority() < safetyPriority) {
                                taskWrapper.exceptionally("Request per minute limit exceeded");
                            } else {
                                TimeUnit.SECONDS.sleep(1);
                                taskWrapper.setPriority(20);
                                requestQueue.offer(taskWrapper);
                            }
                        } else {
                            logger.trace("run for priority {}", taskWrapper.getPriority());
                            taskWrapper.run();
                        }
                    } catch (InterruptedException ie) {
                        logger.info("force termination of the process");
                    } catch (Exception e) {
                        logger.error("an error process task", e);
                    }
                }
            });
        }
    }

    /**
     * Завершает работу всех обслуживающих потоков.
     */
    public void shutdownAll() {
        work.set(false);
        executorService.shutdownNow();
    }

    /**
     * Создает задачу на обработку с указанным приоритетом.
     *
     * @param task        задача которая будет передана в обработку когда подойдет ее очередь
     * @param priority    значение приоритета
     * @param taskProcess интерфейс обработки задачи
     * @return обертка задачи
     */
    public TaskWrapper<TASK, RESULT> createTask(TASK task, int priority, TaskProcess<TASK, RESULT> taskProcess) {
        TaskWrapper<TASK, RESULT> taskWrapper =
            new TaskWrapper<>(task, priority, taskProcess, new CompletableFuture<>());
        requestQueue.offer(taskWrapper);
        return taskWrapper;
    }

    /**
     * Интерфейс обработки задачи.
     *
     * @param <T> тип задачи
     * @param <R> тип результата
     */
    public interface TaskProcess<T, R> {
        R runTask(T req) throws InterruptedException;
    }
}
