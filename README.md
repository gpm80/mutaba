# Ограничитель выполнения задач.
## Multi Tasks Balancer MuTaBa.

Возможности:
- ораничение количества проходимых задач в секунду;
- ораничение количества проходимых задач в минуту;
- приоритетное выполнение отложеных задач;
- возможность установки safetyPriority не гарантирующего выполнение задачи. 

Что такое safetyPriority? 
- Задачи с приориететом ниже safetyPriority не будут выполнены, если не попадают во временной интервал ораничения.
- Задачи с приориететом выше safetyPriority будут ожидать своего выполнения в следующих временных интервалах.

Во многих сторонних web-API имеются ограничения по коичеству запросов за временнЫе рамки.
Если их превышать, то вы можете получить **HTTP 429 Too Many Requests** а при систематическом
превышении возможда и блокировка вашего keyApi.

Чтобы избежать этих "неровностей", вам поможет библиотека MuTaBa.

```java
public static void main(String[] args) throws InterruptedException {
        // Общее количество оперций.
        final int taskCount = 100;
        // Пулл потоков
        final ExecutorService pool = Executors.newCachedThreadPool();
        // Счетчик успешно выполненных задач.
        AtomicInteger success = new AtomicInteger();
        // Замок для ожидания выполнения всех задач.
        CountDownLatch latch = new CountDownLatch(taskCount);
        final Random random = new Random();
        // Инициализачия ограничителя (3 операции в секунду, и 100 операций в минуту.) 
        // Приоритет сохраняемой операции 3 (приортиетты меньше 3 не гарантируют выполнение задачи)
        final TaskLimiter<String, Integer> taskLimiter = new TaskLimiter<>(3, "cals-len-string", 3, 100, 3);
        String[] tasks = new String[] {"aa", "bbbbbb", "ccccccc", "eeeeee", "dddd", "f", "tt", "qqqq", "www"};
        // Создаем поток задач
        for (int i = 0; i < taskCount; i++) {
            pool.execute(() -> {
                final String str = tasks[random.nextInt(tasks.length)];
                final TaskWrapper<String, Integer> task =
                    taskLimiter.createTask(str, random.nextInt(5), val -> {
                        TimeUnit.MILLISECONDS.sleep(random.nextInt(50));
                        return val.length();
                    });
                final Optional<Integer> result = task.waitFor(20, TimeUnit.SECONDS);
                if (result.isPresent()) {
                    success.incrementAndGet();
                    System.out.printf("len: %s = %s %n", str, result.get());
                } else {
                    System.err.printf("len: %s = ignore priority: %s %n", str, task.getPriority());
                }
                latch.countDown();
            });
        }
        // Ожидаем обработки всех заданий
        latch.await();
        pool.shutdown();
        System.out.printf("%s/%s%n", success.get(), taskCount);
    }
```