package com.datastream.config;

import com.datastream.model.Event;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.topic.events}")
    private String eventsTopic;

    @Value("${kafka.topic.events-processed}")
    private String eventsProcessedTopic;

    @Value("${spring.kafka.consumer.concurrency}")
    private int concurrency;

    // Create topics programmatically with optimized settings for high throughput
    @Bean
    public NewTopic eventsTopic() {
        return TopicBuilder.name(eventsTopic)
                .partitions(24)  // Multiple partitions for parallel processing
                .replicas(3)     // Replication for fault tolerance
                .configs(Map.of(
                        "retention.ms", "86400000",  // 24 hours retention
                        "segment.bytes", "1073741824", // 1GB segment size
                        "min.insync.replicas", "2"   // Ensure at least 2 replicas are in sync
                ))
                .build();
    }

    @Bean
    public NewTopic eventsProcessedTopic() {
        return TopicBuilder.name(eventsProcessedTopic)
                .partitions(24)
                .replicas(3)
                .configs(Map.of(
                        "retention.ms", "86400000",
                        "segment.bytes", "1073741824",
                        "min.insync.replicas", "2"
                ))
                .build();
    }

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    // Enhanced Kafka Listener Container Factory with performance optimizations
    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, Event>> kafkaListenerContainerFactory(
            ConsumerFactory<String, Event> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, Event> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(concurrency);
        factory.setBatchListener(true);

        // Configure container properties for better performance
        ContainerProperties props = factory.getContainerProperties();
        props.setPollTimeout(100);
        props.setAckMode(ContainerProperties.AckMode.BATCH);
        props.setIdleEventInterval(60000L);

        return factory;
    }

    // Additional producer configurations for performance
    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768);  // 32KB batch size
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);       // 5ms linger
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 67108864); // 64MB buffer
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        props.put(ProducerConfig.ACKS_CONFIG, "1");          // Less strict for throughput
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        return props;
    }
}