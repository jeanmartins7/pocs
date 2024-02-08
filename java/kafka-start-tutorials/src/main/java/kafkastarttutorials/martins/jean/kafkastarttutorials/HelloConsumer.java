package kafkastarttutorials.martins.jean.kafkastarttutorials;


import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class HelloConsumer {

    @KafkaListener(topics = "hello-topic", groupId = "group-1")
    public void receiveMessage(String message){
        System.out.printf("Consumer message: " + message);
    }
}

