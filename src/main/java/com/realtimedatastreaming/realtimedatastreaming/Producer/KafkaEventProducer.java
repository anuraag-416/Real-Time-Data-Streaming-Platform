package com.example.producer;

import com.example.common.Event;
import org.apache.kafka.clients.producer.*;
import java.util.Properties;
import java.util.UUID;

public class KafkaEventProducer {
    private static final String TOPIC = "events";

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("acks", "1");
        props.put("linger.ms", "5"); // small delay to batch records
        props.put("batch.size", String.valueOf(32 * 1024)); // 32KB batches
        props.put("compression.type", "snappy"); // efficient compression
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        long eventsPerSecond = 10000;
        try {
            while (true) {
                for (int i = 0; i < eventsPerSecond; i++) {
                    Event event = new Event(UUID.randomUUID().toString(), "click", System.currentTimeMillis(), "{\"id\":\"" + UUID.randomUUID() + "\",\"type\":\"click\",\"timestamp\":" + System.currentTimeMillis() + ",\"payload\":\"sample-payload\"}");
                    ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, event.getId(), event.getPayload());
                    producer.send(record);
                }
                Thread.sleep(1000); // Maintain 10K EPS
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            producer.close();
        }
    }
}
