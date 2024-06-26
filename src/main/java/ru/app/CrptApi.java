package ru.app;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.*;

public class CrptApi {

    private static final String DEFAULT_API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final int requestLimit;
    private final TimeUnit timeUnit;
    private final OkHttpClient client = new OkHttpClient();
    private final String apiUrl;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this(timeUnit, requestLimit, DEFAULT_API_URL);
    }

    public CrptApi(TimeUnit timeUnit, int requestLimit, String apiUrl) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.apiUrl = apiUrl;
        this.scheduler.scheduleAtFixedRate(() -> requestCount.set(0), 0, 1, timeUnit);
    }

    public void createDocument(String jsonDocument, String signature) {
        while (requestCount.incrementAndGet() > requestLimit) {
            requestCount.decrementAndGet();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                jsonDocument
        );
        Request request = new Request.Builder()
                .url(apiUrl)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Signature", signature)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            System.out.println(response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
