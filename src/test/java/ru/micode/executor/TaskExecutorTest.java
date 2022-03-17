package ru.micode.executor;

import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Тест обработчика задач.
 */
class TaskExecutorTest {

    /**
     * Тест обработчика с таймаутом.
     */
    @Test
    void testCallMethod() throws TaskExecutor.TaskException {
        Assertions.assertTrue(TaskExecutor.call(1, TimeUnit.SECONDS, () -> {
                TimeUnit.MILLISECONDS.sleep(900);
                return true;
            }
        ));
        Assertions.assertTrue(TaskExecutor.call(200, TimeUnit.MILLISECONDS, () -> {
                TimeUnit.MILLISECONDS.sleep(199);
                return true;
            }
        ));
        try {
            TaskExecutor.call(200, TimeUnit.MILLISECONDS, () -> {
                TimeUnit.MILLISECONDS.sleep(201);
                return true;
            });
            Assertions.fail("Exception expected");
        } catch (Exception e) {
            Assertions.assertNotNull(e);
        }
        try {
            TaskExecutor.call(5, TimeUnit.SECONDS, () -> {
                    TimeUnit.SECONDS.sleep(2);
                    throw new HttpTimeoutException("http error");
                }
            );
            Assertions.fail("Exception expected");
        } catch (Exception e) {
            System.out.println("Exception expected" + e.getMessage());
        }
    }
}