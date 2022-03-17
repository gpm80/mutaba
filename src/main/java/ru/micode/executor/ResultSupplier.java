package ru.micode.executor;

/**
 * Поставщик результатов выполнения.
 */
public interface ResultSupplier<T> {
    /**
     * Возвращает результаты поставщика.
     *
     * @return {@link T}  экземпляр результата
     * @throws Exception исключение, возникшее в момент выполнения операции.
     */
    T getThrow() throws Exception;
}
