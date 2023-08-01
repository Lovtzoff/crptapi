package ru.selsup;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
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
    private final String apiUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final String token = generateNewToken();

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
    private synchronized void makeRequest() throws InterruptedException {
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

    /**
     * Создать документ.
     *
     * @param document     the document
     * @param productGroup the product group
     * @param signature    the signature
     * @throws IOException          the io exception
     * @throws InterruptedException the interrupted exception
     */
    public synchronized void createDocument(Document document, String productGroup, String signature)
            throws IOException, InterruptedException {
        makeRequest();

        String url = apiUrl + "?pg=" + productGroup;

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost httpPost = new HttpPost(url);

        ObjectMapper objectMapper = new ObjectMapper();

        String jsonDocument = objectMapper.writeValueAsString(document);

        RequestBodyToCreateDocument requestBody = RequestBodyToCreateDocument.builder()
                .documentFormat("MANUAL")
                .productDocument(jsonDocument)
                .productGroup(productGroup)
                .signature(signature)
                .type("LP_INTRODUCE_GOODS")
                .build();

        String jsonRequestBody = objectMapper.writeValueAsString(requestBody);

        StringEntity entity = new StringEntity(jsonRequestBody);
        httpPost.setEntity(entity);
        httpPost.setHeader("Authorization", "Bearer " + token);
        httpPost.setHeader("Content-Type", "application/json");

        HttpResponse response = httpClient.execute(httpPost);
        HttpEntity responseEntity = response.getEntity();

        if (responseEntity != null) {
            int responseStatusCode = response.getStatusLine().getStatusCode();
            String responseString = EntityUtils.toString(responseEntity);
            System.out.println("StatusCode: " + responseStatusCode +
                    ",\nResponseEntity: " + responseString);
        }
    }

    /**
     * Сгенерировать случайную строку с токеном в кодировке base64 с 32 символами.
     *
     * @return строка с токеном
     */
    private static String generateNewToken() {
        SecureRandom secureRandom = new SecureRandom();
        Base64.Encoder base64Encoder = Base64.getUrlEncoder();
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }

    /**
     * Класс тела запроса для создания документа.
     */
    @Getter
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Builder
    private static class RequestBodyToCreateDocument {
        final String documentFormat;
        final String productDocument;
        @Setter
        String productGroup;
        final String signature;
        final String type;
    }

    /**
     * The type Description.
     */
    @Getter
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Description {
        final String participantInn;
    }

    /**
     * The type Product.
     */
    @Getter
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Builder
    public static class Product {
        @Setter
        String certificateDocument;
        @Setter
        String certificateDocumentDate;
        @Setter
        String certificateDocumentNumber;
        final String ownerInn;
        final String producerInn;
        final String productionDate;
        final String tnvedCode;
        @Setter
        String uitCode;
        @Setter
        String uituCode;
    }

    /**
     * The type Document.
     */
    @Getter
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Builder
    public static class Document {
        @Setter
        Description description;
        final String docId;
        final String docStatus;
        final String docType;
        @Setter
        String importRequest;
        final String ownerInn;
        final String participantInn;
        final String producerInn;
        final String productionDate;
        final String productionType;
        @Setter
        List<Product> products;
        final String regDate;
        @Setter
        String regNumber;
    }
}
