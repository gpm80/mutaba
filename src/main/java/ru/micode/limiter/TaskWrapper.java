package ru.micode.limiter;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Обертка задачи с Future.
 */
public class TaskWrapper<T, R> implements Comparable<TaskWrapper<T, R>> {

    private static final Logger logger = LoggerFactory.getLogger(TaskWrapper.class);
    private final T task;
    private final CompletableFuture<R> future;
    private final TaskLimiter.TaskProcess<T, R> process;
    private int priority;

    public TaskWrapper(T task, int priority, TaskLimiter.TaskProcess<T, R> taskProcess, CompletableFuture<R> future) {
        this.priority = priority;
        this.task = task;
        this.future = future;
        this.process = taskProcess;
    }

    /**
     * Возвращает результат с временем ожидания.
     *
     * @param time     значение таймаута
     * @param timeUnit единица измерения таймаута
     * @return результат выполнения операции
     */
    public R waitForThrow(long time, TimeUnit timeUnit) throws RuntimeException {
        try {
            return future.get(time, timeUnit);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Возвращает результат операции с временим ожидания.
     *
     * @param time     значение таймаута
     * @param timeUnit единица измерения таймаута
     * @return результат завернутый в {@link Optional}
     */
    public Optional<R> waitFor(long time, TimeUnit timeUnit) {
        try {
            return Optional.of(waitForThrow(time, timeUnit));
        } catch (Exception e) {
            logger.warn("an error", e);
        }
        return Optional.empty();
    }

    /**
     * Запускает обработку задачи.
     */
    public void run() {
        try {
            R r = process.runTask(task);
            future.complete(r);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
    }

    /**
     * Запускает исключение с сообщением об ошибке.
     *
     * @param message сообщение об ошибке
     */
    public void exceptionally(String message) {
        exceptionally(new Exception(message));
    }

    /**
     * Запускает исключение с сообщением об ошибке.
     *
     * @param throwable исключение
     */
    public void exceptionally(Throwable throwable) {
        future.completeExceptionally(throwable);
    }

    /**
     * Возвращает текущий приоритет задачи.
     *
     * @return значение приоритета
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Устаннавливает приоритет задачи.
     *
     * @param priority значение приоритета
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public int compareTo(TaskWrapper o) {
        return o == null
            ? -1
            : Integer.compare(o.priority, priority);
    }
}