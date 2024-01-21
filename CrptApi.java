package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

/**
 * Выполняет запрос Post на адрес
 */
public class CrptApi {
    //Адрес для передачи запроса
    private final URI uri = URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create");
    //семофор для ограничения доступа
    private final Semaphore semaphore;
    //interval время на requestsLimit запросов
    private final long interval;
    //Для освобождения семафора с задержкой
    Timer timer = new Timer(true);

    /**
     * @param requestsLimit количество запорсов
     * @param interval      время на requestsLimit запросов
     */
    public CrptApi(int requestsLimit, long interval) {
        this.interval = interval;
        this.semaphore = new Semaphore(requestsLimit);

    }

    /**
     * @param document Документ который передаем на удрес
     */
    public HttpResponse<String> sendPostDocumentCreate(Document document) throws InterruptedException, IOException {
        //занимает семафор
        semaphore.acquire();
        //переводит обьект в json с помощью Jackson
        ObjectMapper mapper = new ObjectMapper();
        StringWriter stringWriter = new StringWriter();
        mapper.writeValue(stringWriter, document);
        //отправляет запрос
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.ofString(stringWriter.toString()))
                .build();
        //устанавливает обработчик
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                semaphore.release();
            }
        };
        //запускает
        timer.schedule(task, interval);
        //получает и возвращает ответ
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public record Document(Description description,
                           String doc_id,
                           String doc_status,
                           ArrayList<Object> doc_type,
                           boolean importRequest,
                           String owner_inn,
                           String participant_inn,
                           String producer_inn,
                           String production_date,
                           String production_type,
                           ArrayList<Product> products,
                           String reg_date,
                           String reg_number) {
    }

    public record Description(String participantInn) {

    }

    public record Product(String certificate_document,
                          String certificate_document_date,
                          String certificate_document_number,
                          String owner_inn,
                          String producer_inn,
                          String production_date,
                          String tnved_code,
                          String uit_code,
                          String uitu_code) {
    }
}
