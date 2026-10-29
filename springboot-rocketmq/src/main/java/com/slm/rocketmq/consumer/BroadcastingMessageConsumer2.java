package com.slm.rocketmq.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = "broadcasting-message",
        consumerGroup = "broadcasting-group2",
        messageModel = MessageModel.BROADCASTING
)
public class BroadcastingMessageConsumer2 implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        log.info("广播接收者B 收到广播消息：{}", message);
    }

}