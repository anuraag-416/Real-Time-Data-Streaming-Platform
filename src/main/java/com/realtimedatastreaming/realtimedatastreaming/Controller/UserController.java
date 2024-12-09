package com.realtimedatastreaming.realtimedatastreaming.Controller;




import com.realtimedatastreaming.realtimedatastreaming.Model.User;
import com.realtimedatastreaming.realtimedatastreaming.Repository.UserRepository;
import com.realtimedatastreaming.realtimedatastreaming.Service.KafkaConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KafkaConsumerService kafkaConsumerService;

    @GetMapping("/messages")
    public ResponseEntity<List<User>> getMessages() {
        try {
            // Fetch the 10 most recent users from the database
            List<User> users = userRepository.findTop10ByOrderByIdDesc();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            // Log the error and return an internal server error response
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/kafka-messages")
    public ResponseEntity<List<User>> getKafkaMessages() {
        try {
            // This is a simplified version. In a real-world scenario,
            // you might want a more sophisticated message retrieval mechanism
            return ResponseEntity.ok(userRepository.findTop10ByOrderByIdDesc());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
