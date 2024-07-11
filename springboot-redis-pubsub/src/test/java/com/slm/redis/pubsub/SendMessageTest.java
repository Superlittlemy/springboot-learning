package com.slm.redis.pubsub;

import com.slm.redis.pubsub.listener.PublishService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class SendMessageTest {

    @Value("${redis.topic.test}")
    private String testTopic;
    @Resource
    PublishService publishService;

    @Test
    void send() {
        publishService.sendMsg(testTopic, "test message");
        while (true) {}
    }

}
