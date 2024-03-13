package com.slm.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MsgProducer {

    private final KafkaTemplate kafkaTemplate;

    public void send(String message) {
        kafkaTemplate.send("ct", message);
        log.info("生产者发送成功");
    }

}
