package ru.selsup;

import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {

        // Проверка использования класса.
        // В этом примере создается объект CrptApi с ограничением в 10 запросов за 1 секунду и выполняется 20
        // запросов. Если количество запросов превышает ограничение, метод makeRequest() блокируется до тех пор,
        // пока количество запросов не уменьшится до допустимого значения. Если поток прерывается во время
        // блокировки, метод makeRequest() выбрасывает InterruptedException.

        CrptApi api = new CrptApi(TimeUnit.SECONDS, 10);

        for (int i = 0; i < 20; i++) {
            try {
                // сделать запрос API
                api.makeRequest();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}