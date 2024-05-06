package com.warmer.web;

import com.warmer.web.service.impl.KafkaProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class KafkaProducerTest {

    @Autowired
    private KafkaProducer kafkaProducer;

    @Test
    public void testSendMessage() {
        kafkaProducer.sendMessage("Test message");
        // Add assertions to verify the behavior
    }
}
