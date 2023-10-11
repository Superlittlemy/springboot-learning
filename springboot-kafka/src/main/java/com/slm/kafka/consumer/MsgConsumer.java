package com.slm.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@KafkaListener(topics = { "msg" }, groupId = "test-consumer-group")
public class MsgConsumer {

    @KafkaHandler
    public void receive(String message) {
        log.info("消费者接收到的消息：" + message);
    }

}
