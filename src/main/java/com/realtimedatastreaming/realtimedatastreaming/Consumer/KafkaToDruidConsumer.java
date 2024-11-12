package com.example.consumer;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import io.prometheus.client.Counter;
import io.prometheus.client.exporter.HTTPServer;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.*;

public class KafkaToDruidConsumer {
    private static final String TOPIC = "events";
    private static final String DRUID_INGEST_ENDPOINT = "http://localhost:8200/druid/indexer/v1/post";
    private static final int THREAD_POOL_SIZE = 8;
    private static final int BATCH_SIZE = 500;

    // Prometheus metrics
    private static final Counter processedEvents = Counter.build()
            .name("processed_events_total")
            .help("Total processed events.")
            .register();
    private static final Counter failedRequests = Counter.build()
            .name("druid_failed_requests_total")
            .help("Total failed requests to Druid.")
            .register();

    public static void main(String[] args) throws Exception {
        // Prometheus metrics endpoint
        HTTPServer prometheusServer = new HTTPServer(1234);

        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "druid-consumer-group");
        props.put("enable.auto.commit", "true");
        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", StringDeserializer.class.getName());

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(TOPIC));

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<String> batch = new ArrayList<>();

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(100);
            records.forEach(record -> batch.add(record.value()));

            if (batch.size() >= BATCH_SIZE) {
                List<String> batchToSend = new ArrayList<>(batch);
                batch.clear();
                executor.submit(() -> sendToDruid(batchToSend));
            }
        }
    }

    private static void sendToDruid(List<String> events) {
        try {
            URL url = new URL(DRUID_INGEST_ENDPOINT);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            StringBuilder jsonBatch = new StringBuilder();
            jsonBatch.append("[");
            for (int i = 0; i < events.size(); i++) {
                jsonBatch.append(events.get(i));
                if (i != events.size() - 1) jsonBatch.append(",");
            }
            jsonBatch.append("]");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBatch.toString().getBytes());
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                failedRequests.inc();
                System.err.println("Failed to ingest batch. Response: " + responseCode);
            } else {
                processedEvents.inc(events.size());
            }
        } catch (Exception e) {
            failedRequests.inc();
            e.printStackTrace();
        }
    }
}
