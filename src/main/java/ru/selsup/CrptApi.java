package ru.selsup;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Класс для работы с API Честного знака.
 *
 * @author Lovtsov Aliaksei
 */
public class CrptApi {

    private static final int DEFAULT_REQUEST_LIMIT = 100;

    /**
     * Промежуток времени – секунда, минута и пр.
     */
    private final TimeUnit timeUnit;
    /**
     * Максимальное количество запросов в этом промежутке времени.
     */
    private final int requestLimit;

    /**
     * Переменная для хранения количества запросов.
     */
    private final AtomicInteger requestCounter = new AtomicInteger(0);
    /**
     * Переменная для хранения времени последнего запроса.
     */
    private long lastRequestTime = System.currentTimeMillis();

    /**
     * Конструктор по умолчанию.
     */
    public CrptApi() {
        this(TimeUnit.MINUTES, DEFAULT_REQUEST_LIMIT);
    }

    /**
     * Конструктор с параметрами.
     *
     * @param timeUnit     промежуток времени
     * @param requestLimit количество запросов
     */
    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        // проверяем что requestLimit является положительным значением
        if (requestLimit >= 0) {
            this.requestLimit = requestLimit;
        } else {
            throw new IllegalArgumentException("Передано отрицательное число!");
        }
    }

    /**
     * Сделать запрос.
     * Метод выполняет синхронизацию и проверяет, не превышено ли максимальное количество запросов за указанный
     * интервал времени. Если количество запросов превышено, метод блокируется и ждет, пока количество запросов
     * не уменьшится до допустимого значения. Если интервал времени прошел, счетчик запросов сбрасывается.
     *
     * @throws InterruptedException the interrupted exception
     */
    public synchronized void makeRequest() throws InterruptedException {
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastRequestTime;

        if (elapsed >= 1) {
            // если прошло хотя бы 1 единица измерения времени (например, 1 минута),
            // то сбрасываем счетчик запросов и обновляем время последнего запроса
            requestCounter.set(0);
            lastRequestTime = currentTime;
        }

        while (requestCounter.incrementAndGet() > requestLimit) {
            System.out.println("Количество запросов превышено!");
            // ждем, чтобы не превысить максимальное количество запросов к API
            wait(timeUnit.toMillis(1));
            // по истечении задержки сбрасываем счетчик запросов, обновляем время последнего запроса и
            // продолжаем выполнение
            requestCounter.set(0);
            lastRequestTime = System.currentTimeMillis();
        }
    }
}
