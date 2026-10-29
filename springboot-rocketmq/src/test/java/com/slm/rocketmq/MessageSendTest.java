package com.slm.rocketmq;

import com.slm.rocketmq.service.RocketMQService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@SpringBootTest()
public class MessageSendTest {

    @Autowired
    private RocketMQService rocketMQService;

    @Test
    void sendNormalMessage() {
        rocketMQService.syncSendNormalMessage("sync normal message");
        rocketMQService.asyncSendNormalMessage("async normal message");
    }

    @Test
    void sendDelayMessage() {
        LocalDateTime now = LocalDateTime.now();
        rocketMQService.sendDelayMessage("delay message - " + now.format(DateTimeFormatter.ISO_DATE_TIME), 10);
        log.info("send delay message: {}", "delay message - " + now.format(DateTimeFormatter.ISO_DATE_TIME));
    }

    @Test
    void sendOrderlyMessage() {
        String[] orderMessages = new String[] {
                "OrderMessage-001", "OrderMessage-002", "OrderMessage-003"
        };
        rocketMQService.sendOrderlyMessage(orderMessages, "order");
    }

    @Test
    void sendTransactionMessage() {
        rocketMQService.sendTransactionMessage("transaction message");
    }

    @Test
    void sendBroadcastingMessage() {
        rocketMQService.sendBroadcastingMessage("broadcasting message");
    }

    @Test
    void sendTagFilterMessage() {
        rocketMQService.sendTagFilterMessage("tagA message", "tagA");
        rocketMQService.sendTagFilterMessage("tagB message", "tagB");
        rocketMQService.sendTagFilterMessage("tagC message", "tagC");
    }

    @Test
    void sendSQL92FilterMessage() {
        rocketMQService.sendSQL92FilterMessage("sql92 message", Map.of("type", "SQL92"));
    }

}
