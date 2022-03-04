package ru.micode.limiter;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Счетчик с обнулением за интервал времени.
 */
public class TimeLimitCounter {

    private static final Logger logger = LoggerFactory.getLogger(TimeLimitCounter.class);
    private final AtomicInteger counter;
    private final long timeDelay;
    private final TimeUnit timeUnit;
    private final int initValue;
    private String nameCounter;
    private long nextResetCounter;

    /**
     * Конструктор с параметрами.
     *
     * @param resetCounterValue значение счетчика при сбросе
     * @param timeDelay         период сброса счетчика
     * @param timeUnit          единица измерения периода сброса счетчика
     */
    public TimeLimitCounter(int resetCounterValue, long timeDelay, TimeUnit timeUnit) {
        this.nameCounter = timeUnit.name();
        this.initValue = resetCounterValue;
        this.counter = new AtomicInteger(resetCounterValue);
        this.timeDelay = timeDelay;
        this.timeUnit = timeUnit;
    }

    /**
     * Устанавливает название счетчика для лога. Если не указывать, то название установиться из {@link TimeUnit#name()},
     * переданному в конструкторе.
     *
     * @param nameCounter наименование счетчика
     */
    public void setNameCounter(String nameCounter) {
        Optional.ofNullable(nameCounter).ifPresent(name -> this.nameCounter = name);
    }

    public int incrementAndGet() {
        checkAndReset();
        return counter.incrementAndGet();
    }

    public int decrementAndGet() {
        checkAndReset();
        return counter.decrementAndGet();
    }

    public int get() {
        return counter.get();
    }

    private void checkAndReset() {
        synchronized (this) {
            long now = System.currentTimeMillis();
            if (nextResetCounter < now) {
                int val = counter.get();
                if (logger.isDebugEnabled() && val < 0) {
                    logger.debug("decrement counter {} exceeded {}", this.nameCounter, val);
                }
                counter.set(initValue);
                nextResetCounter = now + timeUnit.toMillis(timeDelay);
            }
        }
    }
}
