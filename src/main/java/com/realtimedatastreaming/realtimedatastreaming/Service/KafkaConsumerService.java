package com.realtimedatastreaming.realtimedatastreaming.Service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.realtimedatastreaming.realtimedatastreaming.Model.User;
import com.realtimedatastreaming.realtimedatastreaming.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class KafkaConsumerService {

    private final List<User> batch = new CopyOnWriteArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final int BATCH_SIZE = 10;
    private final long COMMIT_INTERVAL = 10000; // 10 seconds

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private long lastCommitTime = System.currentTimeMillis();

    @KafkaListener(topics = "users_created", groupId = "my-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(String message, Acknowledgment acknowledgment) {
        try {
            User user = objectMapper.readValue(message, User.class);

            lock.lock();
            try {
                batch.add(user);

                long currentTime = System.currentTimeMillis();

                // Process batch if it reaches batch size
                if (batch.size() >= BATCH_SIZE ||
                        (currentTime - lastCommitTime) >= COMMIT_INTERVAL) {
                    processBatch();
                    lastCommitTime = currentTime;
                }
            } finally {
                lock.unlock();
            }

            acknowledgment.acknowledge();
        } catch (IOException e) {
            // Handle parsing error
            e.printStackTrace();
        }
    }

    private void processBatch() {
        if (batch.isEmpty()) return;

        try {
            // Batch insert into database
            userRepository.saveAll(batch);
            System.out.printf("Successfully inserted batch of %d records%n", batch.size());

            // Clear the batch
            batch.clear();
        } catch (Exception e) {
            System.err.printf("Error in batch insert: %s%n", e.getMessage());
            // Optionally, you might want to implement retry or error handling logic
        }
    }

    // Optional method to handle any remaining messages on shutdown
    public void shutdown() {
        processBatch();
    }
}