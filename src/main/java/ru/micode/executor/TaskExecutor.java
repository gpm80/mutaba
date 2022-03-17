package ru.micode.executor;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Исполнитель задач.
 */
public class TaskExecutor {

    /**
     * Запускает обработку с предустановленным временем ожидания.
     *
     * @param timeout      значение времени ожидания обработки
     * @param timeUnit     единица измерения времени
     * @param taskSupplier интерфейс обработки
     * @param <T>          тип возвращаемых данных
     * @return {@link T} экземпляр результата
     * @throws TaskException Ошибка выполнения задачи.
     */
    public static <T> T call(long timeout, TimeUnit timeUnit, ResultSupplier<T> taskSupplier) throws TaskException {
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Future<T> future = executorService.submit(taskSupplier::getThrow);
            return future.get(timeout, timeUnit);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new TaskException(e);
        }
    }

    /**
     * Исключение при обработке задачи.
     */
    public static class TaskException extends Exception {

        public TaskException(Throwable cause) {
            super(cause);
        }
    }
}
